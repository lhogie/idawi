package idawi.service.rest;

class ChunkInfo {
	public String semantic, syntax;
	public int len;
	public int encodedDataLength;

	public ChunkInfo(String semantics, String syntax) {
		this.semantic = semantics;
		this.syntax = syntax;
	}

	public String toJSON() {
		StringBuilder b = new StringBuilder();
		b.append("{");
		var fields = ChunkInfo.class.getDeclaredFields();

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