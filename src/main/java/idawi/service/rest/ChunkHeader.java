package idawi.service.rest;

import java.util.List;

class ChunkHeader {
	public String semantic;
	public List<String> syntax;

	public ChunkHeader(String semantics, List<String> syntax) {
		this.semantic = semantics;
		this.syntax = syntax;
	}

	public String toJSON() {
		/*
		 * StringBuilder b = new StringBuilder(); b.append("{"); var fields =
		 * getClass().getDeclaredFields();
		 * 
		 * for (int i = 0; i < fields.length; ++i) { b.append("\""); var f = fields[i];
		 * b.append(f.getName()); b.append("\": \"");
		 * 
		 * try { b.append(f.get(this).toString()); } catch (IllegalArgumentException |
		 * IllegalAccessException e) { throw new Error(e); }
		 * 
		 * b.append("\"");
		 * 
		 * if (i < fields.length - 1) { b.append(", "); } }
		 * 
		 * b.append("}");
		 * 
		 * return b.toString();
		 */
		StringBuilder b = new StringBuilder();

		for (int i = 0; i < syntax.size(); ++i) {
			b.append(syntax.get(i));

			if (i < syntax.size()) {
				b.append(',');
			}
		}

		return "{\"semantic\": \"" + semantic + "\", \"syntax\": \"" + b + "\"}";
	}
}