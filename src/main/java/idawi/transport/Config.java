package idawi.transport;

import java.util.ArrayList;

public class Config {
	private ArrayList<Param> param;

	public Config() {
		this.param = new ArrayList<>();
	}

	public Config(Config c) {
		this.param = new ArrayList<>(c.param);
	}

	public void modifyConfig(Config c) {
		this.param = new ArrayList<>(c.param);

	}

	public ArrayList<Param> getParams() {
		return param;
	}

	public void setParams(ArrayList<Param> param) {
		this.param = param;
	}

	public void addParam(Param p) {
		this.param.add(p);
	}

	public void removeParam(Param p) {
		this.param.remove(p);
	}

	public boolean modifyParam(String name, int Value) {
		for (Param param2 : param) {
			if (param2.getName().equals(name)) {
				param2.setValue(Value);

				return true;
			}
		}
		return false;
	}

	@Override
	public String toString() {
		return "Config{" +
				"param=" + param +
				'}';
	}
}
