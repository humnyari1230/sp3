package com.sp.guest;

import java.io.Serializable;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name="root")
public class StringData implements Serializable {
	private static final long serialVersionUID = 1L;

	@XmlElement
	private String state;
	
	public StringData() {
		
	}
	
	public StringData(String state) {
		this.state = state;
	}
}
