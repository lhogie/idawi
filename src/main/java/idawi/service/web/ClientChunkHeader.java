package idawi.service.web;

import java.util.List;

import com.fasterxml.jackson.databind.node.ObjectNode;

class ClientChunkHeader extends ChunkHeader {
	public String aim;

	public ClientChunkHeader(String aim, List<String> encodings) {
		super(encodings);
		this.aim = aim;
	}

	@Override
	public ObjectNode toJSONNode() {
		var n = super.toJSONNode();
		n.put("aim", aim);
		return n;
	}

}