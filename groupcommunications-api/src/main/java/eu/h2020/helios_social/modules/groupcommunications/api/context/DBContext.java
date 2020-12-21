package eu.h2020.helios_social.modules.groupcommunications.api.context;

import org.jetbrains.annotations.NotNull;

public class DBContext {

	public String id;
	public String name;
	public Integer color;
	public ContextType contextType;

	public DBContext(@NotNull String id, String name, Integer color,
			ContextType contextType) {
		this.id = id;
		this.name = name;
		this.color = color;
		this.contextType = contextType;
	}

	public String getId() {
		return id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public Integer getColor() {
		return color;
	}

	public void setColor(Integer color) {
		this.color = color;
	}

	public ContextType getContextType(){
		return contextType;
	}
}
