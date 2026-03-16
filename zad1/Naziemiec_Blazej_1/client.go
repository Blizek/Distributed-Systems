package main

import (
	"bufio"
	"fmt"
	"net"
	"os"
	"strings"
)

const (
	SERVER_IP      = "127.0.0.1"
	PORT           = "12345"
	MULTICAST_ADDR = "224.0.0.1:12346"
	ASCII_APT      = `
                   \  |  /         ___________
    ____________  \ \_# /         |  ___      |       _________
   |            |  \  #/          | |   |     |      | = = = = |
   | |   |   |  |   \\#           | |'v'|     |      |         |
   |            |    \#  //       |  --- ___  |      | |  || | |
   | |   |   |  |     #_//        |     |   | |      |         |
   |            |  \\ #_/_______  |     |   | |      | |  || | |
   | |   |   |  |   \\# /_____/ \ |      ---  |      |         |
   |            |    \# |+ ++|  | |  |^^^^^^| |      | |  || | |
   |            |    \# |+ ++|  | |  |^^^^^^| |      | |  || | |
^^^|    (^^^^^) |^^^^^#^| H  |_ |^|  | |||| | |^^^^^^|         |
   |    ( ||| ) |     # ^^^^^^    |  | |||| | |      | ||||||| |
   ^^^^^^^^^^^^^________/  /_____ |  | |||| | |      | ||||||| |
        'v'-                      ^^^^^^^^^^^^^      | ||||||| |
         || |'.      (__)    (__)                          ( )
                     (oo)    (oo)                       /---V
              /-------\/      \/ --------\             * |  |
             / |     ||        ||_______| \
            *  ||W---||        ||      ||  *
               ^^    ^^        ^^      ^^
`
)

func main() {
	fmt.Print("Podaj nick: ")
	scanner := bufio.NewScanner(os.Stdin)
	scanner.Scan()
	nick := scanner.Text()

	tcpConn, errTCPDial := net.Dial("tcp", SERVER_IP+":"+PORT)
	if errTCPDial != nil {
		fmt.Println("Błąd: Serwer nieosiągalny.")
		return
	}

	tcpLocalAddr := tcpConn.LocalAddr().(*net.TCPAddr)

	udpConn, errUDPListen := net.ListenUDP("udp", &net.UDPAddr{IP: net.ParseIP("0.0.0.0"), Port: 0})
	if errUDPListen != nil {
		fmt.Println("Błąd: Nie udało się utworzyć socketu UDP.")
		return
	}
	serverUDPAddr, errResolve := net.ResolveUDPAddr("udp", SERVER_IP+":"+PORT)
	if errResolve != nil {
		fmt.Println("Błąd: Nie udało się rozwiązać adresu UDP serwera.")
		return
	}

	udpLocalAddr := udpConn.LocalAddr().(*net.UDPAddr)

	tcpConn.Write([]byte(nick))
	udpConn.WriteToUDP([]byte("INIT:"+nick), serverUDPAddr)

	fmt.Printf("\n[SYSTEM] Połączono pomyślnie!")
	fmt.Printf("\n[SYSTEM] Lokalny port TCP: %d", tcpLocalAddr.Port)
	fmt.Printf("\n[SYSTEM] Lokalny port UDP: %d", udpLocalAddr.Port)
	fmt.Printf("\n--- ZALOGOWANO JAKO %s ---\n\n", nick)
	showHelp()

	go listenTCP(tcpConn)
	go listenUDP(udpConn)
	go listenMulticast()

	fmt.Print("> ")
	for {
		if !scanner.Scan() {
			break
		}
		input := scanner.Text()

		if input == "exit" {
			break
		}

		if input == "help" {
			showHelp()
			fmt.Print("> ")
		} else {
			processInput(input, nick, tcpConn, udpConn, serverUDPAddr)
		}
	}
	os.Exit(0)
}

func showHelp() {
	fmt.Println("\nDostępne komendy:")
	fmt.Println("  <wiadomość> - Wyślij wiadomość przez TCP")
	fmt.Println("  U <wiadomość> - Wyślij wiadomość przez UDP")
	fmt.Println("  M <wiadomość> - Wyślij wiadomość przez Multicast")
	fmt.Println("  ascii_art - Wyślij ASCII art przez UDP")
	fmt.Println("  exit - Zakończ program")
	fmt.Println("  help - Pokaż tę pomoc\n")
}

func processInput(input, nick string, tcpConn net.Conn, udpConn *net.UDPConn, serverUDPAddr *net.UDPAddr) {
	if strings.HasPrefix(input, "U ") {
		content := strings.TrimPrefix(input, "U ")
		msg := fmt.Sprintf("[%s via UDP] %s", nick, content)
		udpConn.WriteToUDP([]byte(msg), serverUDPAddr)
		fmt.Printf("\r[Wysłałeś via UDP] %s\n>", content)

	} else if strings.HasPrefix(input, "M ") {
		content := strings.TrimPrefix(input, "M ")
		msg := fmt.Sprintf("[%s via Multicast] %s", nick, content)
		sendMulticastMsg(msg)

	} else if input == "ascii_art" {
		msg := fmt.Sprintf("[%s wysyła ASCII art via UDP]:\n%s", nick, ASCII_APT)
		udpConn.WriteToUDP([]byte(msg), serverUDPAddr)
		fmt.Printf("\r[Wysłałeś ASCII art via UDP]\n%s\n>", ASCII_APT)

	} else if len(input) > 0 {
		msg := fmt.Sprintf("[%s via TCP] %s", nick, input)
		tcpConn.Write([]byte(msg))
		fmt.Printf("\r[Wysłałeś via TCP] %s\n>", input)
	}
}

func listenTCP(conn net.Conn) {
	buf := make([]byte, 1024)
	for {
		n, err := conn.Read(buf)
		if err != nil {
			fmt.Println("\n[SYSTEM] Serwer rozłączony.")
			os.Exit(0)
		}
		fmt.Printf("\r%s\n> ", string(buf[:n]))
	}
}

func listenUDP(conn *net.UDPConn) {
	buf := make([]byte, 2048)
	for {
		n, _, err := conn.ReadFromUDP(buf)
		if err != nil {
			fmt.Println("Błąd: Nie udało się odczytać wiadomości UDP.")
			return
		}
		fmt.Printf("\r%s\n> ", string(buf[:n]))
	}
}

func listenMulticast() {
	addr, errResolve := net.ResolveUDPAddr("udp", MULTICAST_ADDR)
	if errResolve != nil {
		fmt.Println("Błąd: Nie udało się rozwiązać adresu UDP multicast.")
		return
	}

	conn, errListen := net.ListenMulticastUDP("udp", nil, addr)
	if errListen != nil {
		fmt.Println("Błąd: Nie udało się utworzyć socketu multicast.")
		return
	}

	if conn == nil {
		return
	}

	buf := make([]byte, 2048)
	for {
		n, _, err := conn.ReadFromUDP(buf)
		if err != nil {
			fmt.Println("Błąd: Nie udało się odczytać wiadomości UDP.")
			return
		}
		fmt.Printf("\r%s\n> ", string(buf[:n]))
	}
}

func sendMulticastMsg(msg string) {
	addr, errResolve := net.ResolveUDPAddr("udp", MULTICAST_ADDR)
	if errResolve != nil {
		fmt.Println("Błąd: Nie udało się rozwiązać adresu UDP multicast.")
		return
	}

	conn, errDial := net.DialUDP("udp", nil, addr)
	if errDial != nil {
		fmt.Println("Błąd: Nie udało się utworzyć socketu UDP.")
		return
	}

	if conn != nil {
		defer conn.Close()
		conn.Write([]byte(msg))
	}
}
