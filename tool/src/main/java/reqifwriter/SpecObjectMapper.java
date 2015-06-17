package reqifwriter;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import reqifwriter.ReqifField.RequirementCall;
import requirement.RequirementProxy;
import requirement.TraceableArtifact;

class SpecObjectMapper {

    enum Type {
	DEFAULT {
	    @Override
	    void setup(final SpecObjectType store) {
		// intentionally do nothing
	    }

	    @Override
	    String getHumanReadableName() {
		return "Requirement Type";
	    }

	    @Override
	    String getIdentifier() {
		return "stype_requirement";
	    }
	},
	PROXY {
	    @Override
	    void setup(final SpecObjectType specObjectType) {
		specObjectType.addField(
			new ReqifFieldProxy("requirementID", String.class, new RequirementCall<RequirementProxy>() {
			    @Override
			    public String call(final RequirementProxy proxyRequirement) {
				assert proxyRequirement != null && proxyRequirement.getHumanReadableManager() != null;
				return proxyRequirement.getHumanReadableManager().getTag();
			    }
			}));
	    }

	    @Override
	    String getHumanReadableName() {
		return "Proxy Type";
	    }

	    @Override
	    String getIdentifier() {
		return "stype_requirementproxy";
	    }
	};

	abstract String getHumanReadableName();
	abstract String getIdentifier();
	abstract void setup(final SpecObjectType store); 
    }    
    final Map<Type, SpecObjectType<TraceableArtifact>> map = new HashMap<>(Type.values().length); 

    public SpecObjectMapper() {
	for (final Type currentType : Type.values()) {
	    final SpecObjectType<TraceableArtifact> specObjectType = new SpecObjectType<>(currentType.getIdentifier(), currentType.getHumanReadableName());
	    currentType.setup(specObjectType);
	    this.map.put(currentType, specObjectType);
	}
    }

    SpecObjectType<TraceableArtifact> getType(final Type type) {
	return this.map.get(type);
    }
    
    Collection<SpecObjectType<TraceableArtifact>> getAvailableTypes() {
	return this.map.values();
    }
}