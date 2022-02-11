package idawi;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class StupidTest2 {

	public static void main(String[] args) throws IOException {
		var i = new BufferedReader(new InputStreamReader(System.in));

		while (true) {
			var l = i.readLine();

			if (l == null)
				System.exit(0);

			if (l.contains("a"))
				System.out.println(l);
		}
	}

}
