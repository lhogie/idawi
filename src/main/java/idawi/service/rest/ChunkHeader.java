package idawi.service.rest;

class ChunkHeader {
	public String semantic, syntax;
	public int len;
	public int encodedDataLength;

	public ChunkHeader(String semantics, String syntax) {
		this.semantic = semantics;
		this.syntax = syntax;
	}

	public String toJSON() {
		StringBuilder b = new StringBuilder();
		b.append("{");
		var fields = ChunkHeader.class.getDeclaredFields();

		for (int i = 0; i < fields.length; ++i) {
			b.append("\"");
			var f = fields[i];
			b.append(f.getName());
			b.append("\": \"");

			try {
				b.append(f.get(this).toString());
			} catch (IllegalArgumentException | IllegalAccessException e) {
				throw new Error(e);
			}

			b.append("\"");

			if (i < fields.length - 1) {
				b.append(", ");
			}
		}

		b.append("}");

		return b.toString();
//		return "{\"semantic\": \"" + semantic + "\", \"syntax\": \"" + syntax + "\", \"length\": " + len + "}";
	}
}