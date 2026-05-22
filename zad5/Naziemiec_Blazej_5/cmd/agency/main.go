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

	reader := bufio.NewReader(os.Stdin)
	var agencyName string
	var q amqp.Queue

	fmt.Println("Rejestracja Agencji Kosmicznej")

	for {
		fmt.Print("Podaj unikalną nazwę Agencji: ")
		nameInput, _ := reader.ReadString('\n')
		agencyName = strings.TrimSpace(nameInput)

		if agencyName == "" {
			fmt.Println("[BŁĄD] Nazwa nie może być pusta.")
			continue
		}

		qName := "q_agency_" + agencyName
		var err error

		q, err = ch.QueueDeclare(qName, false, true, true, false, nil)

		if err != nil {
			fmt.Printf("[BŁĄD] Nazwa '%s' jest już zajęta w systemie. Wybierz inną.\n", agencyName)
			ch.Close()
			ch, _ = conn.Channel()
			continue
		}
		break
	}

	ch.QueueBind(q.Name, "agency."+agencyName, mq.ExchangeName, false, nil)
	ch.QueueBind(q.Name, "admin.agencies", mq.ExchangeName, false, nil)
	ch.QueueBind(q.Name, "admin.all", mq.ExchangeName, false, nil)

	msgs, _ := ch.Consume(q.Name, "", true, false, false, false, nil)

	go func() {
		for d := range msgs {
			var msg models.Message
			json.Unmarshal(d.Body, &msg)

			if msg.Type == "confirmation" {
				fmt.Printf("\n>>> [%s] OTRZYMANO POTWIERDZENIE od %s dla zlecenia %s <<<\n> ", agencyName, msg.CarrierName, msg.OrderID)
			} else if msg.Type == "admin" {
				fmt.Printf("\n>>> [%s] WIADOMOŚĆ ADMINISTRACYJNA: %s <<<\n> ", agencyName, msg.Content)
			}
		}
	}()

	fmt.Printf("\n[%s] System online.\n", agencyName)

	orderCounter := 1
	for {
		fmt.Println("\nZleć nową usługę:")
		fmt.Println("1 - Przewóz osób (people)")
		fmt.Println("2 - Przewóz ładunku (cargo)")
		fmt.Println("3 - Umieszczenie satelity na orbicie (satellite)")
		fmt.Print("> ")

		choice, _ := reader.ReadString('\n')
		choice = strings.TrimSpace(choice)

		var serviceType string
		switch choice {
		case "1":
			serviceType = "people"
		case "2":
			serviceType = "cargo"
		case "3":
			serviceType = "satellite"
		default:
			fmt.Println("[BŁĄD] Nieprawidłowy wybór. Wpisz 1, 2 lub 3.")
			continue
		}

		orderID := fmt.Sprintf("ORDER_%s_%d", agencyName, orderCounter)

		sendRequest(ch, agencyName, orderID, serviceType)
		orderCounter++
	}
}

func sendRequest(ch *amqp.Channel, agencyName, orderID, serviceType string) {
	msg := models.Message{
		Type:        "request",
		AgencyName:  agencyName,
		OrderID:     orderID,
		ServiceType: serviceType,
	}
	body, _ := json.Marshal(msg)

	ch.PublishWithContext(context.Background(), mq.ExchangeName, "service."+serviceType, false, false, amqp.Publishing{
		ContentType: "application/json",
		Body:        body,
	})
	fmt.Printf("[SYSTEM] Wysłano %s (typ: %s)\n", orderID, serviceType)
}
