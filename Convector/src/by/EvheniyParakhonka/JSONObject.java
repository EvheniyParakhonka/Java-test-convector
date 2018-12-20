package by.EvheniyParakhonka;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.*;
import java.util.Map.Entry;
import java.util.regex.Pattern;

public class JSONObject {

    private static final class Null {


        @Override
        protected final Object clone() {
            return this;
        }

        @Override
        public boolean equals(Object object) {
            return object == null || object == this;
        }

        @Override
        public int hashCode() {
            return 0;
        }

        @Override
        public String toString() {
            return "null";
        }
    }
    
    static final Pattern NUMBER_PATTERN = Pattern.compile("-?(?:0|[1-9]\\d*)(?:\\.\\d+)?(?:[eE][+-]?\\d+)?");

    private final Map<String, Object> map;

    public static final Object NULL = new Null();

    public JSONObject() {

        this.map = new HashMap<>();
    }

    public JSONObject(JSONObject pJSONObject, String[] pNames) {
        this(pNames.length);
        for (int i = 0; i < pNames.length; i += 1) {
            try {
                this.putOnce(pNames[i], pJSONObject.chooseObject(pNames[i]));
            } catch (Exception ignore) {
            }
        }
    }

    public JSONObject(JSONTokener pTokener) throws JSONException {
        this();
        char c;
        String key;

        if (pTokener.nextClean() != '{') {
            throw pTokener.syntaxError("A JSONObject text must begin with '{'");
        }
        for (;;) {
            c = pTokener.nextClean();
            switch (c) {
            case 0:
                throw pTokener.syntaxError("A JSONObject text must end with '}'");
            case '}':
                return;
            default:
                pTokener.back();
                key = pTokener.nextValue().toString();
            }

            // The key is followed by ':'.

            c = pTokener.nextClean();
            if (c != ':') {
                throw pTokener.syntaxError("Expected a ':' after a key");
            }
            
            // Use syntaxError(..) to include error location
            
            if (key != null) {
                // Check if key exists
                if (this.chooseObject(key) != null) {
                    // key already exists
                    throw pTokener.syntaxError("Duplicate key \"" + key + "\"");
                }
                // Only add value if non-null
                Object value = pTokener.nextValue();
                if (value!=null) {
                    this.put(key, value);
                }
            }

            // Pairs are separated by ','.

            switch (pTokener.nextClean()) {
            case ';':
            case ',':
                if (pTokener.nextClean() == '}') {
                    return;
                }
                pTokener.back();
                break;
            case '}':
                return;
            default:
                throw pTokener.syntaxError("Expected a ',' or '}'");
            }
        }
    }

    public JSONObject(Map<?, ?> m) {
        if (m == null) {
            this.map = new HashMap<>();
        } else {
            this.map = new HashMap<>(m.size());
        	for (final Entry<?, ?> e : m.entrySet()) {
        	    if(e.getKey() == null) {
        	        throw new NullPointerException("Null key.");
        	    }
                final Object value = e.getValue();
                if (value != null) {
                    this.map.put(String.valueOf(e.getKey()), wrap(value));
                }
            }
        }
    }


    public JSONObject(Object pObject, String[] pNames) {
        this(pNames.length);
        Class<?> c = pObject.getClass();
        for (int i = 0; i < pNames.length; i += 1) {
            String name = pNames[i];
            try {
                this.putOpt(name, c.getField(name).get(pObject));
            } catch (Exception ignore) {
            }
        }
    }

    public JSONObject(String pString) throws JSONException {
        this(new JSONTokener(pString));
    }

    public JSONObject(String pBaseName, Locale pLocale) throws JSONException {
        this();
        ResourceBundle bundle = ResourceBundle.getBundle(pBaseName, pLocale,
                Thread.currentThread().getContextClassLoader());

        Enumeration<String> keys = bundle.getKeys();
        while (keys.hasMoreElements()) {
            Object key = keys.nextElement();
            if (key != null) {

                String[] path = ((String) key).split("\\.");
                int last = path.length - 1;
                JSONObject target = this;
                for (int i = 0; i < last; i += 1) {
                    String segment = path[i];
                    JSONObject nextTarget = target.chooseJSONObject(segment);
                    if (nextTarget == null) {
                        nextTarget = new JSONObject();
                        target.put(segment, nextTarget);
                    }
                    target = nextTarget;
                }
                target.put(path[last], bundle.getString((String) key));
            }
        }
    }

    protected JSONObject(int pInitialCapacity){
        this.map = new HashMap<String, Object>(pInitialCapacity);
    }

    public JSONObject accumulate(String pKey, Object pValue) throws JSONException {
        testValidity(pValue);
        Object object = this.chooseObject(pKey);
        if (object == null) {
            this.put(pKey,
                    pValue instanceof JSONArray ? new JSONArray().put(pValue)
                            : pValue);
        } else if (object instanceof JSONArray) {
            ((JSONArray) object).put(pValue);
        } else {
            this.put(pKey, new JSONArray().put(object).put(pValue));
        }
        return this;
    }

    public JSONObject append(String pKey, Object pValue) throws JSONException {
        testValidity(pValue);
        Object object = this.chooseObject(pKey);
        if (object == null) {
            this.put(pKey, new JSONArray().put(pValue));
        } else if (object instanceof JSONArray) {
            this.put(pKey, ((JSONArray) object).put(pValue));
        } else {
            throw new JSONException("JSONObject[" + pKey
                    + "] is not a JSONArray.");
        }
        return this;
    }

    public static String doubleToString(double pDouble) {
        if (Double.isInfinite(pDouble) || Double.isNaN(pDouble)) {
            return "null";
        }

// Shave off trailing zeros and decimal point, if possible.

        String string = Double.toString(pDouble);
        if (string.indexOf('.') > 0 && string.indexOf('e') < 0
                && string.indexOf('E') < 0) {
            while (string.endsWith("0")) {
                string = string.substring(0, string.length() - 1);
            }
            if (string.endsWith(".")) {
                string = string.substring(0, string.length() - 1);
            }
        }
        return string;
    }

    public Object getObject(String pKey) throws JSONException {
        if (pKey == null) {
            throw new JSONException("Null key.");
        }
        Object object = this.chooseObject(pKey);
        if (object == null) {
            throw new JSONException("JSONObject[" + quote(pKey) + "] not found.");
        }
        return object;
    }

    public boolean getBoolean(String pKey) throws JSONException {
        Object object = this.getObject(pKey);
        if (object.equals(Boolean.FALSE)
                || (object instanceof String && ((String) object)
                        .equalsIgnoreCase("false"))) {
            return false;
        } else if (object.equals(Boolean.TRUE)
                || (object instanceof String && ((String) object)
                        .equalsIgnoreCase("true"))) {
            return true;
        }
        throw new JSONException("JSONObject[" + quote(pKey)
                + "] is not a Boolean.");
    }

    public BigInteger getBigInteger(String pKey) throws JSONException {
        Object object = this.getObject(pKey);
        BigInteger ret = objectToBigInteger(object, null);
        if (ret != null) {
            return ret;
        }
        throw new JSONException("JSONObject[" + quote(pKey)
                + "] could not be converted to BigInteger (" + object + ").");
    }

    public BigDecimal getBigDecimal(String pKey) throws JSONException {
        Object object = this.getObject(pKey);
        BigDecimal ret = objectToBigDecimal(object, null);
        if (ret != null) {
            return ret;
        }
        throw new JSONException("JSONObject[" + quote(pKey)
                + "] could not be converted to BigDecimal (" + object + ").");
    }

    public double getDouble(String pKey) throws JSONException {
        return this.getNumber(pKey).doubleValue();
    }

    public float getFloat(String pKey) throws JSONException {
        return this.getNumber(pKey).floatValue();
    }

    public Number getNumber(String pKey) throws JSONException {
        Object object = this.getObject(pKey);
        try {
            if (object instanceof Number) {
                return (Number)object;
            }
            return stringToNumber(object.toString());
        } catch (Exception e) {
            throw new JSONException("JSONObject[" + quote(pKey)
                    + "] is not a number.", e);
        }
    }

    public int getInt(String pKey) throws JSONException {
        return this.getNumber(pKey).intValue();
    }

    public JSONArray getJSONArray(String pKey) throws JSONException {
        Object object = this.getObject(pKey);
        if (object instanceof JSONArray) {
            return (JSONArray) object;
        }
        throw new JSONException("JSONObject[" + quote(pKey)
                + "] is not a JSONArray.");
    }

    public JSONObject getJSONObject(String pKey) throws JSONException {
        Object object = this.getObject(pKey);
        if (object instanceof JSONObject) {
            return (JSONObject) object;
        }
        throw new JSONException("JSONObject[" + quote(pKey)
                + "] is not a JSONObject.");
    }

    public long getLong(String pKey) throws JSONException {
        return this.getNumber(pKey).longValue();
    }

    public static String[] getNames(JSONObject pJSONObject) {
        if (pJSONObject.isEmpty()) {
            return null;
        }
        return pJSONObject.keySet().toArray(new String[pJSONObject.length()]);
    }

    public static String[] getNames(Object pObject) {
        if (pObject == null) {
            return null;
        }
        Class<?> klass = pObject.getClass();
        Field[] fields = klass.getFields();
        int length = fields.length;
        if (length == 0) {
            return null;
        }
        String[] names = new String[length];
        for (int i = 0; i < length; i += 1) {
            names[i] = fields[i].getName();
        }
        return names;
    }

    public String getString(String pKey) throws JSONException {
        Object object = this.getObject(pKey);
        if (object instanceof String) {
            return (String) object;
        }
        throw new JSONException("JSONObject[" + quote(pKey) + "] not a string.");
    }

    public boolean isMapHasKey(String pKey) {
        return this.map.containsKey(pKey);
    }

    public JSONObject increment(String pKey) throws JSONException {
        Object value = this.chooseObject(pKey);
        if (value == null) {
            this.put(pKey, 1);
        } else if (value instanceof BigInteger) {
            this.put(pKey, ((BigInteger)value).add(BigInteger.ONE));
        } else if (value instanceof BigDecimal) {
            this.put(pKey, ((BigDecimal)value).add(BigDecimal.ONE));
        } else if (value instanceof Integer) {
            this.put(pKey, ((Integer) value).intValue() + 1);
        } else if (value instanceof Long) {
            this.put(pKey, ((Long) value).longValue() + 1L);
        } else if (value instanceof Double) {
            this.put(pKey, ((Double) value).doubleValue() + 1.0d);
        } else if (value instanceof Float) {
            this.put(pKey, ((Float) value).floatValue() + 1.0f);
        } else {
            throw new JSONException("Unable to increment [" + quote(pKey) + "].");
        }
        return this;
    }

    public boolean isNull(String pKey) {
        return JSONObject.NULL.equals(this.chooseObject(pKey));
    }

    public Iterator<String> keys() {
        return this.keySet().iterator();
    }

    public Set<String> keySet() {
        return this.map.keySet();
    }

    protected Set<Entry<String, Object>> entrySet() {
        return this.map.entrySet();
    }

    public int length() {
        return this.map.size();
    }

    public boolean isEmpty() {
        return this.map.isEmpty();
    }

    public JSONArray names() {
    	if(this.map.isEmpty()) {
    		return null;
    	}
        return new JSONArray(this.map.keySet());
    }

    public static String numberToString(Number pNumber) throws JSONException {
        if (pNumber == null) {
            throw new JSONException("Null pointer");
        }
        testValidity(pNumber);

        // Shave off trailing zeros and decimal point, if possible.

        String string = pNumber.toString();
        if (string.indexOf('.') > 0 && string.indexOf('e') < 0
                && string.indexOf('E') < 0) {
            while (string.endsWith("0")) {
                string = string.substring(0, string.length() - 1);
            }
            if (string.endsWith(".")) {
                string = string.substring(0, string.length() - 1);
            }
        }
        return string;
    }

    public Object chooseObject(String pKey) {
        return pKey == null ? null : this.map.get(pKey);
    }

    public boolean chooseBoolean(String pKey) {
        return this.chooseBoolean(pKey, false);
    }

    public boolean chooseBoolean(String pKey, boolean pDefaultvalue) {
        Object val = this.chooseObject(pKey);
        if (NULL.equals(val)) {
            return pDefaultvalue;
        }
        if (val instanceof Boolean){
            return (Boolean) val;
        }
        try {
            // we'll use the getObject anyway because it does string conversion.
            return this.getBoolean(pKey);
        } catch (Exception e) {
            return pDefaultvalue;
        }
    }

    public BigDecimal chooseBigDecimal(String key, BigDecimal defaultValue) {
        Object val = this.chooseObject(key);
        return objectToBigDecimal(val, defaultValue);
    }

    static BigDecimal objectToBigDecimal(Object val, BigDecimal defaultValue) {
        if (NULL.equals(val)) {
            return defaultValue;
        }
        if (val instanceof BigDecimal){
            return (BigDecimal) val;
        }
        if (val instanceof BigInteger){
            return new BigDecimal((BigInteger) val);
        }
        if (val instanceof Double || val instanceof Float){
            final double d = ((Number) val).doubleValue();
            if(Double.isNaN(d)) {
                return defaultValue;
            }
            return new BigDecimal(((Number) val).doubleValue());
        }
        if (val instanceof Long || val instanceof Integer
                || val instanceof Short || val instanceof Byte){
            return new BigDecimal(((Number) val).longValue());
        }
        // don't check if it's a string in case of unchecked Number subclasses
        try {
            return new BigDecimal(val.toString());
        } catch (Exception e) {
            return defaultValue;
        }
    }

    public BigInteger chooseBigInteger(String key, BigInteger defaultValue) {
        Object val = this.chooseObject(key);
        return objectToBigInteger(val, defaultValue);
    }

    static BigInteger objectToBigInteger(Object val, BigInteger defaultValue) {
        if (NULL.equals(val)) {
            return defaultValue;
        }
        if (val instanceof BigInteger){
            return (BigInteger) val;
        }
        if (val instanceof BigDecimal){
            return ((BigDecimal) val).toBigInteger();
        }
        if (val instanceof Double || val instanceof Float){
            final double d = ((Number) val).doubleValue();
            if(Double.isNaN(d)) {
                return defaultValue;
            }
            return new BigDecimal(d).toBigInteger();
        }
        if (val instanceof Long || val instanceof Integer
                || val instanceof Short || val instanceof Byte){
            return BigInteger.valueOf(((Number) val).longValue());
        }
        // don't check if it's a string in case of unchecked Number subclasses
        try {
            final String valStr = val.toString();
            if(isDecimalNotation(valStr)) {
                return new BigDecimal(valStr).toBigInteger();
            }
            return new BigInteger(valStr);
        } catch (Exception e) {
            return defaultValue;
        }
    }

    public double chooseDouble(String pKey) {
        return this.chooseDouble(pKey, Double.NaN);
    }

    public double chooseDouble(String pKey, double pDefaultValue) {
        Number val = this.chooseNumber(pKey);
        if (val == null) {
            return pDefaultValue;
        }
        final double doubleValue = val.doubleValue();
        // if (Double.isNaN(doubleValue) || Double.isInfinite(doubleValue)) {
        // return defaultValue;
        // }
        return doubleValue;
    }

    public float chooseFloat(String pKey) {
        return this.chooseFloat(pKey, Float.NaN);
    }

    public float chooseFloat(String pKey, float pDefaultValue) {
        Number val = this.chooseNumber(pKey);
        if (val == null) {
            return pDefaultValue;
        }
        final float floatValue = val.floatValue();
        // if (Float.isNaN(floatValue) || Float.isInfinite(floatValue)) {
        // return defaultValue;
        // }
        return floatValue;
    }

    public int chooseInt(String pKey) {
        return this.chooseInt(pKey, 0);
    }

    public int chooseInt(String pKey, int pDefaultValue) {
        final Number val = this.chooseNumber(pKey, null);
        if (val == null) {
            return pDefaultValue;
        }
        return val.intValue();
    }

    public JSONArray chooseJSONArray(String pKey) {
        Object o = this.chooseObject(pKey);
        return o instanceof JSONArray ? (JSONArray) o : null;
    }

    public JSONObject chooseJSONObject(String pKey) {
        Object object = this.chooseObject(pKey);
        return object instanceof JSONObject ? (JSONObject) object : null;
    }

    public long chooseLong(String key) {
        return this.chooseLong(key, 0);
    }

    public long chooseLong(String key, long defaultValue) {
        final Number val = this.chooseNumber(key, null);
        if (val == null) {
            return defaultValue;
        }
        
        return val.longValue();
    }
    
    public Number chooseNumber(String pKey) {
        return this.chooseNumber(pKey, null);
    }

    public Number chooseNumber(String pKey, Number pDefaultValue) {
        Object val = this.chooseObject(pKey);
        if (NULL.equals(val)) {
            return pDefaultValue;
        }
        if (val instanceof Number){
            return (Number) val;
        }
        
        try {
            return stringToNumber(val.toString());
        } catch (Exception e) {
            return pDefaultValue;
        }
    }
    
    public String chooseString(String pKey) {
        return this.chooseString(pKey, "");
    }

    public String chooseString(String pKey, String pDefaultValue) {
        Object object = this.chooseObject(pKey);
        return NULL.equals(object) ? pDefaultValue : object.toString();
    }

    private boolean isValidMethodName(String pName) {
        return !"getClass".equals(pName) && !"getDeclaringClass".equals(pName);
    }

    public JSONObject put(String key, boolean value) throws JSONException {
        return this.put(key, value ? Boolean.TRUE : Boolean.FALSE);
    }

    public JSONObject put(String key, Collection<?> value) throws JSONException {
        return this.put(key, new JSONArray(value));
    }

    public JSONObject put(String key, double value) throws JSONException {
        return this.put(key, Double.valueOf(value));
    }
    
    public JSONObject put(String key, float value) throws JSONException {
        return this.put(key, Float.valueOf(value));
    }

    public JSONObject put(String key, int value) throws JSONException {
        return this.put(key, Integer.valueOf(value));
    }

    public JSONObject put(String key, long value) throws JSONException {
        return this.put(key, Long.valueOf(value));
    }

    public JSONObject put(String key, Map<?, ?> value) throws JSONException {
        return this.put(key, new JSONObject(value));
    }

    public JSONObject put(String key, Object value) throws JSONException {
        if (key == null) {
            throw new NullPointerException("Null key.");
        }
        if (value != null) {
            testValidity(value);
            this.map.put(key, value);
        } else {
            this.remove(key);
        }
        return this;
    }

    public JSONObject putOnce(String key, Object value) throws JSONException {
        if (key != null && value != null) {
            if (this.chooseObject(key) != null) {
                throw new JSONException("Duplicate key \"" + key + "\"");
            }
            return this.put(key, value);
        }
        return this;
    }

    public JSONObject putOpt(String key, Object value) throws JSONException {
        if (key != null && value != null) {
            return this.put(key, value);
        }
        return this;
    }

    public static String quote(String string) {
        StringWriter sw = new StringWriter();
        synchronized (sw.getBuffer()) {
            try {
                return quote(string, sw).toString();
            } catch (IOException ignored) {
                return "";
            }
        }
    }

    public static Writer quote(String string, Writer w) throws IOException {
        if (string == null || string.isEmpty()) {
            w.write("\"\"");
            return w;
        }

        char b;
        char c = 0;
        String hhhh;
        int i;
        int len = string.length();

        w.write('"');
        for (i = 0; i < len; i += 1) {
            b = c;
            c = string.charAt(i);
            switch (c) {
            case '\\':
            case '"':
                w.write('\\');
                w.write(c);
                break;
            case '/':
                if (b == '<') {
                    w.write('\\');
                }
                w.write(c);
                break;
            case '\b':
                w.write("\\b");
                break;
            case '\t':
                w.write("\\t");
                break;
            case '\n':
                w.write("\\n");
                break;
            case '\f':
                w.write("\\f");
                break;
            case '\r':
                w.write("\\r");
                break;
            default:
                if (c < ' ' || (c >= '\u0080' && c < '\u00a0')
                        || (c >= '\u2000' && c < '\u2100')) {
                    w.write("\\u");
                    hhhh = Integer.toHexString(c);
                    w.write("0000", 0, 4 - hhhh.length());
                    w.write(hhhh);
                } else {
                    w.write(c);
                }
            }
        }
        w.write('"');
        return w;
    }

    public Object remove(String key) {
        return this.map.remove(key);
    }

    public boolean similar(Object other) {
        try {
            if (!(other instanceof JSONObject)) {
                return true;
            }
            if (!this.keySet().equals(((JSONObject)other).keySet())) {
                return true;
            }
            for (final Entry<String,?> entry : this.entrySet()) {
                String name = entry.getKey();
                Object valueThis = entry.getValue();
                Object valueOther = ((JSONObject)other).getObject(name);
                if(valueThis == valueOther) {
                	continue;
                }
                if(valueThis == null) {
                	return true;
                }
                if (valueThis instanceof JSONObject) {
                    if (((JSONObject) valueThis).similar(valueOther)) {
                        return true;
                    }
                } else if (valueThis instanceof JSONArray) {
                    if (((JSONArray) valueThis).similar(valueOther)) {
                        return true;
                    }
                } else if (!valueThis.equals(valueOther)) {
                    return true;
                }
            }
            return false;
        } catch (Throwable exception) {
            return true;
        }
    }
    
    protected static boolean isDecimalNotation(final String val) {
        return val.indexOf('.') > -1 || val.indexOf('e') > -1
                || val.indexOf('E') > -1 || "-0".equals(val);
    }
    
    protected static Number stringToNumber(final String val) throws NumberFormatException {
        char initial = val.charAt(0);
        if ((initial >= '0' && initial <= '9') || initial == '-') {
            if (isDecimalNotation(val)) {
                if (val.length()>14) {
                    return new BigDecimal(val);
                }
                final Double d = Double.valueOf(val);
                if (d.isInfinite() || d.isNaN()) {
                    return new BigDecimal(val);
                }
                return d;
            }

            BigInteger bi = new BigInteger(val);
            if(bi.bitLength()<=31){
                return Integer.valueOf(bi.intValue());
            }
            if(bi.bitLength()<=63){
                return Long.valueOf(bi.longValue());
            }
            return bi;
        }
        throw new NumberFormatException("val ["+val+"] is not a valid number.");
    }

    public static Object stringToValue(String string) {
        if ("".equals(string)) {
            return string;
        }

        // check JSON key words true/false/null
        if ("true".equalsIgnoreCase(string)) {
            return Boolean.TRUE;
        }
        if ("false".equalsIgnoreCase(string)) {
            return Boolean.FALSE;
        }
        if ("null".equalsIgnoreCase(string)) {
            return JSONObject.NULL;
        }



        char initial = string.charAt(0);
        if ((initial >= '0' && initial <= '9') || initial == '-') {
            try {

                if (isDecimalNotation(string)) {
                    Double d = Double.valueOf(string);
                    if (!d.isInfinite() && !d.isNaN()) {
                        return d;
                    }
                } else {
                    Long myLong = Long.valueOf(string);
                    if (string.equals(myLong.toString())) {
                        if (myLong.longValue() == myLong.intValue()) {
                            return Integer.valueOf(myLong.intValue());
                        }
                        return myLong;
                    }
                }
            } catch (Exception ignore) {
            }
        }
        return string;
    }

    public static void testValidity(Object o) throws JSONException {
        if (o != null) {
            if (o instanceof Double) {
                if (((Double) o).isInfinite() || ((Double) o).isNaN()) {
                    throw new JSONException(
                            "JSON does not allow non-finite numbers.");
                }
            } else if (o instanceof Float) {
                if (((Float) o).isInfinite() || ((Float) o).isNaN()) {
                    throw new JSONException(
                            "JSON does not allow non-finite numbers.");
                }
            }
        }
    }

    @Override
    public String toString() {
        try {
            return this.toString(0);
        } catch (Exception e) {
            return null;
        }
    }

    public String toString(int indentFactor) throws JSONException {
        StringWriter w = new StringWriter();
        synchronized (w.getBuffer()) {
            return this.write(w, indentFactor, 0).toString();
        }
    }

    public static String valueToString(Object value) throws JSONException {

        return JSONWriter.valueToString(value);
    }

    public static Object wrap(Object object) {
        try {
            if (object == null) {
                return NULL;
            }
            if (object instanceof JSONObject || object instanceof JSONArray
                    || NULL.equals(object) || object instanceof JSONString
                    || object instanceof Byte || object instanceof Character
                    || object instanceof Short || object instanceof Integer
                    || object instanceof Long || object instanceof Boolean
                    || object instanceof Float || object instanceof Double
                    || object instanceof String || object instanceof BigInteger
                    || object instanceof BigDecimal || object instanceof Enum) {
                return object;
            }

            if (object instanceof Collection) {
                Collection<?> coll = (Collection<?>) object;
                return new JSONArray(coll);
            }
            if (object.getClass().isArray()) {
                return new JSONArray(object);
            }
            if (object instanceof Map) {
                Map<?, ?> map = (Map<?, ?>) object;
                return new JSONObject(map);
            }
            Package objectPackage = object.getClass().getPackage();
            String objectPackageName = objectPackage != null ? objectPackage
                    .getName() : "";
            if (objectPackageName.startsWith("java.")
                    || objectPackageName.startsWith("javax.")
                    || object.getClass().getClassLoader() == null) {
                return object.toString();
            }
            return new JSONObject((JSONTokener) object);
        } catch (Exception exception) {
            return null;
        }
    }

    public Writer write(Writer writer) throws JSONException {
        return this.write(writer, 0, 0);
    }

    static final Writer writeValue(Writer writer, Object value,
            int indentFactor, int indent) throws JSONException, IOException {
        if (value == null || value.equals(null)) {
            writer.write("null");
        } else if (value instanceof JSONString) {
            Object o;
            try {
                o = ((JSONString) value).toJSONString();
            } catch (Exception e) {
                throw new JSONException(e);
            }
            writer.write(o != null ? o.toString() : quote(value.toString()));
        } else if (value instanceof Number) {
            final String numberAsString = numberToString((Number) value);
            if(NUMBER_PATTERN.matcher(numberAsString).matches()) {
                writer.write(numberAsString);
            } else {
                quote(numberAsString, writer);
            }
        } else if (value instanceof Boolean) {
            writer.write(value.toString());
        } else if (value instanceof Enum<?>) {
            writer.write(quote(((Enum<?>)value).name()));
        } else if (value instanceof JSONObject) {
            ((JSONObject) value).write(writer, indentFactor, indent);
        } else if (value instanceof JSONArray) {
            ((JSONArray) value).write(writer, indentFactor, indent);
        } else if (value instanceof Map) {
            Map<?, ?> map = (Map<?, ?>) value;
            new JSONObject(map).write(writer, indentFactor, indent);
        } else if (value instanceof Collection) {
            Collection<?> coll = (Collection<?>) value;
            new JSONArray(coll).write(writer, indentFactor, indent);
        } else if (value.getClass().isArray()) {
            new JSONArray(value).write(writer, indentFactor, indent);
        } else {
            quote(value.toString(), writer);
        }
        return writer;
    }

    static final void indent(Writer writer, int indent) throws IOException {
        for (int i = 0; i < indent; i += 1) {
            writer.write(' ');
        }
    }

    public Writer write(Writer writer, int indentFactor, int indent)
            throws JSONException {
        try {
            boolean commanate = false;
            final int length = this.length();
            writer.write('{');

            if (length == 1) {
            	final Entry<String,?> entry = this.entrySet().iterator().next();
                final String key = entry.getKey();
                writer.write(quote(key));
                writer.write(':');
                if (indentFactor > 0) {
                    writer.write(' ');
                }
                try{
                    writeValue(writer, entry.getValue(), indentFactor, indent);
                } catch (Exception e) {
                    throw new JSONException("Unable to write JSONObject value for key: " + key, e);
                }
            } else if (length != 0) {
                final int newindent = indent + indentFactor;
                for (final Entry<String,?> entry : this.entrySet()) {
                    if (commanate) {
                        writer.write(',');
                    }
                    if (indentFactor > 0) {
                        writer.write('\n');
                    }
                    indent(writer, newindent);
                    final String key = entry.getKey();
                    writer.write(quote(key));
                    writer.write(':');
                    if (indentFactor > 0) {
                        writer.write(' ');
                    }
                    try {
                        writeValue(writer, entry.getValue(), indentFactor, newindent);
                    } catch (Exception e) {
                        throw new JSONException("Unable to write JSONObject value for key: " + key, e);
                    }
                    commanate = true;
                }
                if (indentFactor > 0) {
                    writer.write('\n');
                }
                indent(writer, indent);
            }
            writer.write('}');
            return writer;
        } catch (IOException exception) {
            throw new JSONException(exception);
        }
    }

}
