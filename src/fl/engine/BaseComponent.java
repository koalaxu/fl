package fl.engine;

import fl.engine.data.DataAccessor;

public class BaseComponent {
	protected BaseComponent(DataAccessor data_accessor, ComponentHub component_hub) {
		this.data_accessor_ = data_accessor;
		this.component_hub_ = component_hub;
	}
	
	protected DataAccessor GetAccessor() {
		return data_accessor_;
	}
	
	protected ComponentHub GetComponentHub() {
		return component_hub_;
	}
	
	private DataAccessor data_accessor_;	
	private ComponentHub component_hub_;
}
