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

	q, _ := ch.QueueDeclare("q_admin", true, false, false, false, nil)
	ch.QueueBind(q.Name, "#", mq.ExchangeName, false, nil)
	msgs, _ := ch.Consume(q.Name, "", true, false, false, false, nil)

	go func() {
		for d := range msgs {
			var msg models.Message
			json.Unmarshal(d.Body, &msg)
			if msg.Type != "admin" {
				fmt.Printf("\n[SNOOP] Przechwycono: typ=%s, agency=%s, usługa=%s\n> ", msg.Type, msg.AgencyName, msg.ServiceType)
			}
		}
	}()

	reader := bufio.NewReader(os.Stdin)
	for {
		fmt.Print("\nWybierz cel (1: Agencje, 2: Przewoźnicy, 3: Wszyscy): ")
		target, _ := reader.ReadString('\n')
		target = strings.TrimSpace(target)

		routingKey := "admin.all"
		switch target {
		case "1":
			routingKey = "admin.agencies"
		case "2":
			routingKey = "admin.carriers"
		}

		fmt.Print("Treść wiadomości: ")
		content, _ := reader.ReadString('\n')
		content = strings.TrimSpace(content)

		msg := models.Message{Type: "admin", Content: content}
		body, _ := json.Marshal(msg)

		ch.PublishWithContext(context.Background(), mq.ExchangeName, routingKey, false, false, amqp.Publishing{
			ContentType: "application/json",
			Body:        body,
		})
		fmt.Println("[SUCCESS] Wysłano wiadomość administracyjną.")
	}
}
