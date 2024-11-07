package idawi.service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

import idawi.Component;
import idawi.FunctionEndPoint;
import idawi.Service;

/**
 * Sends an empty message on a queue that is created specifically for the peer
 * to bench.
 */

public class Auth extends Service {

	public static class User {
		private String loginName;

		public User(String login) {
			this.loginName = login;
		}

		public boolean isAdmin() {
			return loginName.equals("admin");
		}
	}

	public Auth(Component node) {
		super(node);
	}

	@Override
	public String getFriendlyName() {
		return "auth";
	}

	public static File inputFile;

	public class authFromFile extends FunctionEndPoint<String, User> {

		@Override
		public String getDescription() {
			return "check if the given user/passwd is valid";
		}

		@Override
		public User f(String testLoginPasswd) throws IOException {
			for (var line : Files.readAllLines(inputFile.toPath())) {
				if (testLoginPasswd.equals(line)) {
					return new User(testLoginPasswd);
				}
			}

			return null;
		}
	}
}
