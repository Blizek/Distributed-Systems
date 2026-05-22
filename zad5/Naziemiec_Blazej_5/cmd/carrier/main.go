package main

import (
	"bufio"
	"context"
	"encoding/json"
	"fmt"
	"os"
	"strings"

	"space-delivery/internal/models"
	"space-delivery/internal/mq"

	amqp "github.com/rabbitmq/amqp091-go"
)

func main() {
	conn, ch := mq.Connect()
	defer conn.Close()
	defer ch.Close()

	mq.SetupServiceQueues(ch)

	reader := bufio.NewReader(os.Stdin)
	var carrierName string
	var qAdmin amqp.Queue

	fmt.Println("Rejestracja przewoźnika kosmicznego")

	for {
		fmt.Print("Podaj unikalną nazwę przewoźnika: ")
		nameInput, _ := reader.ReadString('\n')
		carrierName = strings.TrimSpace(nameInput)

		if carrierName == "" {
			fmt.Println("[BŁĄD] Nazwa nie może być pusta.")
			continue
		}

		qAdminName := "q_carrier_" + carrierName
		var err error

		qAdmin, err = ch.QueueDeclare(qAdminName, false, true, true, false, nil)

		if err != nil {
			fmt.Printf("[BŁĄD] Nazwa '%s' jest już zajęta w systemie. Wybierz inną.\n", carrierName)
			ch.Close()
			ch, _ = conn.Channel()
			continue
		}
		break
	}

	servicesMap := map[string]string{
		"1": "people",
		"2": "cargo",
		"3": "satellite",
	}

	var srv1, srv2 string
	for {
		fmt.Println("\nDostępne typy usług transportowych:")
		fmt.Println("1 - Przewóz osób (people)")
		fmt.Println("2 - Przewóz ładunku (cargo)")
		fmt.Println("3 - Umieszczenie satelity na orbicie (satellite)")

		fmt.Print("Wybierz pierwszą usługę (1-3): ")
		s1Input, _ := reader.ReadString('\n')
		s1 := strings.TrimSpace(s1Input)

		fmt.Print("Wybierz drugą usługę (1-3): ")
		s2Input, _ := reader.ReadString('\n')
		s2 := strings.TrimSpace(s2Input)

		if s1 == s2 {
			fmt.Println("[BŁĄD] Wybrane usługi muszą być różne! Spróbuj ponownie.")
			continue
		}

		var ok1, ok2 bool
		srv1, ok1 = servicesMap[s1]
		srv2, ok2 = servicesMap[s2]

		if !ok1 || !ok2 {
			fmt.Println("[BŁĄD] Nieprawidłowy wybór. Użyj cyfr 1, 2 lub 3.")
			continue
		}
		break
	}

	ch.Qos(1, 0, false)

	ch.QueueBind(qAdmin.Name, "admin.carriers", mq.ExchangeName, false, nil)
	ch.QueueBind(qAdmin.Name, "admin.all", mq.ExchangeName, false, nil)
	msgsAdmin, _ := ch.Consume(qAdmin.Name, "", true, false, false, false, nil)

	msgs1, _ := ch.Consume("q_service_"+srv1, "", false, false, false, false, nil)
	msgs2, _ := ch.Consume("q_service_"+srv2, "", false, false, false, false, nil)

	fmt.Printf("\n[%s] System online. Oczekuję na zlecenia typów: %s, %s...\n", carrierName, srv1, srv2)

	go func() {
		for d := range msgsAdmin {
			var msg models.Message
			json.Unmarshal(d.Body, &msg)
			fmt.Printf("\n[%s] WIADOMOŚĆ ADMINISTRACYJNA: %s\n", carrierName, msg.Content)
		}
	}()

	processRequest := func(d amqp.Delivery) {
		var msg models.Message
		json.Unmarshal(d.Body, &msg)

		fmt.Printf("\n[%s] Rozpoczęto realizację zlecenia %s dla %s (%s)...\n", carrierName, msg.OrderID, msg.AgencyName, msg.ServiceType)

		// Uncomment to show QoS is working
		// sleepSeconds := 1 + rand.Intn(3)
		// fmt.Printf("[%s] Realizuję %s... czas: %d sekund\n", carrierName, msg.ServiceType, sleepSeconds)
		// time.Sleep(time.Duration(sleepSeconds) * time.Second)

		confMsg := models.Message{
			Type:        "confirmation",
			AgencyName:  msg.AgencyName,
			OrderID:     msg.OrderID,
			CarrierName: carrierName,
			ServiceType: msg.ServiceType,
		}
		confBody, _ := json.Marshal(confMsg)

		ch.PublishWithContext(context.Background(), mq.ExchangeName, "agency."+msg.AgencyName, false, false, amqp.Publishing{
			ContentType: "application/json",
			Body:        confBody,
		})

		d.Ack(false)
		fmt.Printf("[%s] Zakończono %s. Wysłano potwierdzenie do %s.\n", carrierName, msg.OrderID, msg.AgencyName)
	}

	for {
		select {
		case d := <-msgs1:
			processRequest(d)
		case d := <-msgs2:
			processRequest(d)
		}
	}
}
