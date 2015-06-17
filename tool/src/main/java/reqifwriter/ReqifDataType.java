package reqifwriter;

import helper.RegexHelper;

import java.util.Locale;

class ReqifDataType<T> {    
    private final Class<T> javaDatatype;
    private final Datatype reqifDatatype;
    
    enum Datatype {
	INTEGER {
	    @Override
	    public String toString() {
		return "Integer";
	    }
	},
	STRING {
	    @Override
	    public String toString() {
		return "String";
	    }
	},
	ENUM {
	    @Override
	    public String toString() {
		return "Enumeration";
	    }
	},
	BOOLEAN {
	    @Override
	    public String toString() {
		return "Boolean";
	    }
	},
	XHTML {
	    @Override
	    public String toString() {
		return "XHTML";
	    }
	}
    }

    ReqifDataType(final Class<T> javaDatatype, final String reqifDatatype) {
	if (javaDatatype == null) throw new IllegalArgumentException("javaDatatype cannot be null.");
	// reqifDatatype can be null
	this.javaDatatype = javaDatatype;	
	this.reqifDatatype = computeEnumType(reqifDatatype);
    }
    
    ReqifDataType(final Class<T> javaDataType) {
	this(javaDataType, null);
    }

    String getReqifDatatypeTag() {	
	return "DATATYPE-DEFINITION-" + getTagAppendix();
    }

    Datatype getReqifDatatypeIdentifierRaw() {
	return this.reqifDatatype;
    }
    
    String getReqifDatatypeIdentifier() {
	return "_dtype_" + getDatatypeEnumAware();
    }
    
    String getReqifDatatypeIdentifier(final String enumValue) {
	return "_enumVal_" + getDatatypeEnumAware() +'_' + enumValue;
    }

    String getReqifDatatypeLongName() {
	return "T_" + getDatatypeEnumAware();
    }    

    String[][] getEnumerationNames() {
	final Enum<?>[] enumValues = (Enum<?>[]) this.javaDatatype.getEnumConstants();
	final String[][] output = new String[enumValues.length][2]; 
	for (int i = 0; i < output.length; i++) {
	    output[i] = new String[]{enumValues[i].name(), enumValues[i].toString()};
	}
	return output;
    }        
    
    /**
     * <em>Note:</em> In contrast to {@link #getReqifDatatypeIdentifierRaw()} this does resolve enumerations (and thus returns their individual class name)
     * 
     * @return the datatype with enums resolved or the user-defined type, if set
     */
    String getDatatypeEnumAware() {	
	return this.reqifDatatype == Datatype.ENUM ? this.javaDatatype.getSimpleName() : this.reqifDatatype.toString();  	
    }
    
    protected String getTagAppendix() {
	return this.reqifDatatype.toString().toUpperCase(Locale.ENGLISH);
    }
    
    /**
     * @param userDefined user defined datatype or {@code null} if undefined
     * @return the enum representation of the type represented by this object
     * @throws IllegalArgumentException if the datatype cannot be handled
     * @throws IllegalStateException if the {@link #javaDatatype} behaves strangely
     */
    private Datatype computeEnumType(final String userDefined) {
	final Datatype output;
	final String javaName;
	if (userDefined != null) {
	    if ("XHTML".equals(userDefined)) output = Datatype.XHTML;
	    else throw new IllegalArgumentException("user-defined datatype is unknown");
	}	    
	else if ((javaName = this.javaDatatype.getCanonicalName()) != null) {
	    final String trimmendJavaName;
	    if (this.javaDatatype.isEnum()) {
		output = Datatype.ENUM;
	    }
	    else if((trimmendJavaName = RegexHelper.extractRegex(javaName, "java\\.lang\\.([^\\.]+)$")) != null) {
		switch(trimmendJavaName) {
		case "String":
		    output = Datatype.STRING; break;
		case "Integer":
		    output = Datatype.INTEGER; break;
		case "Boolean":
		    output = Datatype.BOOLEAN; break;
		default:
		    throw new IllegalStateException("Encountered an unknown datatype.");
		}		
	    }	    
	    else throw new IllegalArgumentException("Encountered a datatype which is neither an enum nor a standard java type. Cannot handle that.");
	}
	else throw new IllegalStateException("Could not determine datatype of object for XML serialization.");
	return output;	
    }
}
