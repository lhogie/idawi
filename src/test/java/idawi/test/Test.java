package idawi.test;

import java.lang.reflect.ParameterizedType;
import java.util.ArrayList;

public class Test {

	public static void main(String[] args) throws Throwable {

		class E extends ArrayList<String>{
			
		}
		
		
		var s = Class.forName(((ParameterizedType) E.class.getGenericSuperclass()).getActualTypeArguments()[0].getTypeName()).getConstructor().newInstance();
		
		System.out.println(s.getClass());
	}

}