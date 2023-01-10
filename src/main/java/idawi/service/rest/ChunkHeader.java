package idawi.service.rest;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

import toools.text.TextUtilities;

class ChunkHeader {
	public List<String> encodings;

	public ChunkHeader(List<String> encodings) {
		this.encodings = encodings;
	}

	public ObjectNode toJSONNode() {
		var f = new JsonNodeFactory(false);
		ObjectNode n = new ObjectNode(f);
		n.put("encodings", TextUtilities.concat(", ", encodings, e -> e));
		return n;
	}

	public static void main(String[] args) {
		System.out.println("test");
		var l = new ArrayList<String>();
		l.add("fds");
		ChunkHeader h = new ChunkHeader(l);
		var s = h.toJSONNode().toString();
		System.out.println(s.length());
		System.out.println(s);
	}

}