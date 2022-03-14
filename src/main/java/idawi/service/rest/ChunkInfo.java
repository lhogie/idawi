package idawi.service.rest;

class ChunkInfo {
	String semantic, syntax;
	int len;

	public ChunkInfo(String semantics, String syntax) {
		this.semantic = semantics;
		this.syntax = syntax;
	}

	public String toJSON() {
		return "{\"semantic\": \"" + semantic + "\", \"syntax\": \"" + syntax + "\", \"length\": " + len + "}";
	}
}