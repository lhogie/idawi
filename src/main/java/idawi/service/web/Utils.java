package idawi.service.web;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

import com.fasterxml.jackson.databind.JsonNode;
import com.sun.net.httpserver.HttpExchange;

import idawi.messaging.MessageCollector;
import idawi.routing.RoutingParameters;
import idawi.routing.RoutingService;
import toools.io.ser.Serializer;
import toools.text.TextUtilities;

public class Utils {
	static void controlCollect(JsonNode header, MessageCollector collector) {
		var newEndDate = header.get("endDate");

		if (newEndDate != null) {
			collector.endDate = newEndDate.asDouble();
		}

		var shouldStop = header.get("stop");

		if (shouldStop != null) {
			collector.gotEnough = shouldStop.asBoolean();
		}
	}

	static List<String> path(String s) {
		if (s == null) {
			return null;
		}

		if (s.startsWith("/")) {
			s = s.substring(1);
		}

		// if the path ends by a /
		if (s.charAt(s.length() - 1) == '/' && s.charAt(s.length() - 2) != '/') {
			s = s.substring(0, s.length() - 1);
		}

		return s.isEmpty() ? null : TextUtilities.split(s, '/');
	}

	static String removeOrDefault(Map<String, String> map, String k, String defaultValue, Set<String> validKeys) {
		var r = map.remove(k);

		if (r == null)
			r = defaultValue;

		if (validKeys != null && !validKeys.contains(r))
			throw new IllegalArgumentException(
					r + " is not a valid value for '" + k + "'. Valid values are: " + validKeys);

		return r;
	}

	static Map<String, String> query(String s) {
		Map<String, String> query = new HashMap<>();

		if (s != null && !s.isEmpty()) {
			for (String queryEntry : TextUtilities.split(s, '&')) {
				String[] a = queryEntry.split("=");

				if (a.length == 2) {
					query.put(a[0], a[1]);
				} else {
					query.put(a[0], null);
				}
			}
		}

		return query;
	}

	static void sendEvent(OutputStream out, ChunkHeader header, byte[] data, boolean base64) throws IOException {
		out.write("data: ".getBytes());
		out.write(header.toJSONNode().toString().getBytes());
		out.write('\n');

		if (base64) {
			data = TextUtilities.base64(data).getBytes();
		}

		var dataText = TextUtilities.prefixEachLineBy(new String(data), "data: ");
		out.write(dataText.getBytes());
		out.write('\n');
		out.write('\n'); // end of event
		out.flush();
	}

	static String guessMIMEType(List<String> path) {
		if (path == null || path.isEmpty()) {
			return "application/octet-stream";
		}
		var p = path.get(path.size() - 1);
		try {
			var r = Files.probeContentType(Path.of(p));
			return r == null ? "application/octet-stream" : r;
		} catch (IOException e) {
			return "application/octet-stream";
		}
	}

	static void singleHTTPResponse(int returnCode, String mimeType, byte[] bytes, HttpExchange e, OutputStream os)
			throws IOException {
		e.getResponseHeaders().set("Content-type", mimeType);
		e.sendResponseHeaders(returnCode, bytes.length);
		os.write(bytes);
	}

	static Predicate<MessageCollector> parseGotEnoughCondition(String s) {
		if (s == null) {
			return c -> false;
		} else if (s.matches("[0-9]+msg")) {
			int nbMsg = Integer.valueOf(s.substring(0, s.length() - 3));
			return c -> c.messages.size() >= nbMsg;
		} else if (s.matches("[0-9]+senders")) {
			int nbSenders = Integer.valueOf(s.substring(0, s.length() - 6));
			return c -> c.messages.senders().size() >= nbSenders;
		} else if (s.matches("[0-9]+eot")) {
			int n = Integer.valueOf(s.substring(0, s.length() - 3));
			return c -> c.messages.eots().size() >= n;
		} else if (s.matches("[0-9]+errors")) {
			int n = Integer.valueOf(s.substring(0, s.length() - 6));
			return c -> c.messages.errorMessages().size() >= n;
		}

		throw new IllegalArgumentException("cannot interpret: " + s);
	}

	static <P extends RoutingParameters> RoutingParameters routingParms(Map<String, String> query, RoutingService<P> r,
			OutputStream output, Serializer serializer) throws IOException {
		if (query.containsKey("rp")) {
			final RoutingParameters rp = r.defaultData();
			rp.fromString(query.remove("rp"), r);
			return rp;
		} else {
			new Suggestion("rp", "specifies the parameters of the routing protocol", r.dataSuggestions()).send(output,
					serializer);
			return r.defaultData();
		}
	}
}
