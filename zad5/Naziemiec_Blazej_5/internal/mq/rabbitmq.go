package mq

import (
	"log"

	amqp "github.com/rabbitmq/amqp091-go"
)

const ExchangeName = "space_exchange"

func FailOnError(err error, msg string) {
	if err != nil {
		log.Fatalf("%s: %s", msg, err)
	}
}

func Connect() (*amqp.Connection, *amqp.Channel) {
	conn, err := amqp.Dial("amqp://guest:guest@localhost:5672/")
	FailOnError(err, "Nie udało się połączyć z RabbitMQ")

	ch, err := conn.Channel()
	FailOnError(err, "Nie udało się otworzyć kanału")

	err = ch.ExchangeDeclare(ExchangeName, "topic", true, false, false, false, nil)
	FailOnError(err, "Nie udało się zadeklarować exchange")

	return conn, ch
}

func SetupServiceQueues(ch *amqp.Channel) {
	services := []string{"people", "cargo", "satellite"}
	for _, svc := range services {
		q, err := ch.QueueDeclare("q_service_"+svc, true, false, false, false, nil)
		FailOnError(err, "Błąd deklaracji kolejki usług")
		err = ch.QueueBind(q.Name, "service."+svc, ExchangeName, false, nil)
		FailOnError(err, "Błąd bindowania kolejki usług")
	}
}
