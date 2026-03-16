package main

import (
	"fmt"
	"net"
	"os"
	"os/signal"
	"strings"
	"sync"
	"syscall"
)

const PORT = "12345"

type Server struct {
	clientsTCP map[net.Conn]string
	clientsUDP map[string]*net.UDPAddr
	mu         sync.Mutex
}

func main() {
	s := &Server{
		clientsTCP: make(map[net.Conn]string),
		clientsUDP: make(map[string]*net.UDPAddr),
	}

	sigs := make(chan os.Signal, 1)
	signal.Notify(sigs, syscall.SIGINT, syscall.SIGTERM)

	go s.startUDP()
	go s.startTCP()

	fmt.Println("--- SERWER URUCHOMIONY ---")

	<-sigs
	fmt.Println("\n[SERVER] Zamykanie...")
	os.Exit(0)
}

func (s *Server) startTCP() {
	ln, err := net.Listen("tcp", ":"+PORT)
	if err != nil {
		fmt.Println("Błąd: Nie udało się uruchomić serwera TCP.")
		os.Exit(1)
	}
	for {
		conn, err := ln.Accept()
		if err != nil {
			fmt.Println("Błąd: Nie udało się zaakceptować połączenia TCP.")
			continue
		}
		go s.handleTCP(conn)
	}
}

func (s *Server) handleTCP(conn net.Conn) {
	addr := conn.RemoteAddr().String()
	buf := make([]byte, 1024)

	n, err := conn.Read(buf)
	if err != nil {
		conn.Close()
		return
	}
	nick := string(buf[:n])

	s.mu.Lock()
	s.clientsTCP[conn] = nick
	s.mu.Unlock()

	fmt.Printf("[TCP] Klient %s został zarejestrowany na %s\n", nick, addr)

	defer func() {
		s.mu.Lock()
		delete(s.clientsTCP, conn)
		s.mu.Unlock()
		fmt.Printf("[SERVER] %s (%s) rozłączony z TCP.\n", nick, addr)
		conn.Close()
	}()

	for {
		n, err := conn.Read(buf)
		if err != nil {
			return
		}
		s.broadcastTCP(string(buf[:n]), conn)
	}
}

func (s *Server) broadcastTCP(msg string, exclude net.Conn) {
	s.mu.Lock()
	defer s.mu.Unlock()
	for client := range s.clientsTCP {
		if client != exclude {
			client.Write([]byte(msg))
		}
	}
}

func (s *Server) startUDP() {
	addr, errResolve := net.ResolveUDPAddr("udp", ":"+PORT)
	if errResolve != nil {
		fmt.Println("Błąd: Nie udało się rozwiązać adresu UDP.")
		os.Exit(1)
	}
	conn, errListen := net.ListenUDP("udp", addr)
	if errListen != nil {
		fmt.Println("Błąd: Nie udało się uruchomić serwera UDP.")
		os.Exit(1)
	}
	buf := make([]byte, 2048)

	for {
		n, remoteAddr, err := conn.ReadFromUDP(buf)
		if err != nil {
			continue
		}
		msg := string(buf[:n])
		addrKey := remoteAddr.String()

		s.mu.Lock()
		if strings.HasPrefix(msg, "INIT:") {
			nick := strings.TrimPrefix(msg, "INIT:")
			s.clientsUDP[addrKey] = remoteAddr

			fmt.Printf("[UDP] Klient %s został zarejestrowany na %s\n", nick, addrKey)
		} else {
			for key, clientAddr := range s.clientsUDP {
				if key != addrKey {
					conn.WriteToUDP(buf[:n], clientAddr)
				}
			}
		}
		s.mu.Unlock()
	}
}
