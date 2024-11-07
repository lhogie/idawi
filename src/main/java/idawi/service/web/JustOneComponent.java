package idawi.service.web;

import idawi.Component;
import idawi.Idawi;

public class JustOneComponent {

	public static void main(String[] args) throws Throwable {
		var c = new Component();
		var ws = c.need(WebService.class);
		ws.startHTTPServer();
		System.out.println("listening on port " + ws.DEFAULT_PORT);

		Idawi.agenda.start();
		Idawi.agenda.stopWhen(() -> false, null);

//		Desktop.getDesktop().browse(new URI("http://localhost:8081/"));
	}
}