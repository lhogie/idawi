package idawi;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Random;

import toools.text.TextUtilities;

public class StupidTest {

	public static void main(String[] args) throws IOException {
		var p = Runtime.getRuntime().exec("java " + StupidTest2.class.getName());

		var t = new Thread(() -> {
			var in = new BufferedReader(new InputStreamReader(p.getInputStream()));

			while (true) {
				try {
					var i = in.readLine();

					if (i == null)
						return;

					System.out.println("match: " + i);
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		});

		 t.start();

		var out = p.getOutputStream();

		for (int i = 0;; ++i) {
			var s = TextUtilities.pickRandomString(new Random(), 10, 10);
			out.write(s.getBytes());
			out.write('\n');
			System.out.println(i + " worked!: " + s);
		}
	}

}
