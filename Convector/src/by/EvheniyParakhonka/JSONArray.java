package by.EvheniyParakhonka;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.lang.reflect.Array;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;

public class JSONArray implements Iterable<Object> {

    private final ArrayList<Object> myArrayList;

    public JSONArray() {
        this.myArrayList = new ArrayList<>();
    }

    public JSONArray(JSONTokener pTokener) throws JSONException {
        this();
        if (pTokener.nextClean() != '[') {
            throw pTokener.syntaxError("A JSONArray text must start with '['");
        }

        char nextChar = pTokener.nextClean();
        if (nextChar == 0) {
            // array is unclosed. No ']' found, instead EOF
            throw pTokener.syntaxError("Expected a ',' or ']'");
        }
        if (nextChar != ']') {
            pTokener.back();
            for (; ; ) {
                if (pTokener.nextClean() == ',') {
                    pTokener.back();
                    this.myArrayList.add(JSONObject.NULL);
                } else {
                    pTokener.back();
                    this.myArrayList.add(pTokener.nextValue());
                }
                switch (pTokener.nextClean()) {
                    case 0:
                        throw pTokener.syntaxError("Expected a ',' or ']'");
                    case ',':
                        nextChar = pTokener.nextClean();
                        if (nextChar == 0) {
                            throw pTokener.syntaxError("Expected a ',' or ']'");
                        }
                        if (nextChar == ']') {
                            return;
                        }
                        pTokener.back();
                        break;
                    case ']':
                        return;
                    default:
                        throw pTokener.syntaxError("Expected a ',' or ']'");
                }
            }
        }
    }

    public JSONArray(String pValue) throws JSONException {
        this(new JSONTokener(pValue));
    }

    public JSONArray(Collection<?> pObjects) {
        if (pObjects == null) {
            this.myArrayList = new ArrayList<Object>();
        } else {
            this.myArrayList = new ArrayList<Object>(pObjects.size());
            for (Object o : pObjects) {
                this.myArrayList.add(JSONObject.wrap(o));
            }
        }
    }

    public JSONArray(Object array) throws JSONException {
        this();
        if (array.getClass().isArray()) {
            int length = Array.getLength(array);
            this.myArrayList.ensureCapacity(length);
            for (int i = 0; i < length; i += 1) {
                this.put(JSONObject.wrap(Array.get(array, i)));
            }
        } else {
            throw new JSONException(
                    "JSONArray initial value should be a string or collection or array.");
        }
    }

    @Override
    public Iterator<Object> iterator() {
        return this.myArrayList.iterator();
    }

    public Object getObject(int pIndex) throws JSONException {
        Object object = this.chosse(pIndex);
        if (object == null) {
            throw new JSONException("JSONArray[" + pIndex + "] not found.");
        }
        return object;
    }

    public boolean getBoolean(int pIndex) throws JSONException {
        Object object = this.getObject(pIndex);
        if (object.equals(Boolean.FALSE)
                || (object instanceof String && ((String) object)
                .equalsIgnoreCase("false"))) {
            return false;
        } else if (object.equals(Boolean.TRUE)
                || (object instanceof String && ((String) object)
                .equalsIgnoreCase("true"))) {
            return true;
        }
        throw new JSONException("JSONArray[" + pIndex + "] is not a boolean.");
    }

    public double getDouble(int pIndex) throws JSONException {
        return this.getNumber(pIndex).doubleValue();
    }

    public float getFloat(int pIndex) throws JSONException {
        return this.getNumber(pIndex).floatValue();
    }

    public Number getNumber(int pIndex) throws JSONException {
        Object object = this.getObject(pIndex);
        try {
            if (object instanceof Number) {
                return (Number) object;
            }
            return JSONObject.stringToNumber(object.toString());
        } catch (Exception e) {
            throw new JSONException("JSONArray[" + pIndex + "] is not a number.", e);
        }
    }

    public BigDecimal getBigDecimal(int pIndex) throws JSONException {
        Object object = this.getObject(pIndex);
        BigDecimal val = JSONObject.objectToBigDecimal(object, null);
        if (val == null) {
            throw new JSONException("JSONArray[" + pIndex +
                    "] could not convert to BigDecimal (" + object + ").");
        }
        return val;
    }

    public BigInteger getBigInteger(int pIndex) throws JSONException {
        Object object = this.getObject(pIndex);
        BigInteger val = JSONObject.objectToBigInteger(object, null);
        if (val == null) {
            throw new JSONException("JSONArray[" + pIndex +
                    "] could not convert to BigDecimal (" + object + ").");
        }
        return val;
    }

    public int getInt(int pIndex) throws JSONException {
        return this.getNumber(pIndex).intValue();
    }

    public JSONArray getJSONArray(int pIndex) throws JSONException {
        Object object = this.getObject(pIndex);
        if (object instanceof JSONArray) {
            return (JSONArray) object;
        }
        throw new JSONException("JSONArray[" + pIndex + "] is not a JSONArray.");
    }

    public JSONObject getJSONObject(int pIndex) throws JSONException {
        Object object = this.getObject(pIndex);
        if (object instanceof JSONObject) {
            return (JSONObject) object;
        }
        throw new JSONException("JSONArray[" + pIndex + "] is not a JSONObject.");
    }

    public long getLong(int pIndex) throws JSONException {
        return this.getNumber(pIndex).longValue();
    }

    public String getString(int pIndex) throws JSONException {
        Object object = this.getObject(pIndex);
        if (object instanceof String) {
            return (String) object;
        }
        throw new JSONException("JSONArray[" + pIndex + "] not a string.");
    }

    public boolean isNull(int pIndex) {
        return JSONObject.NULL.equals(this.chosse(pIndex));
    }

    public int length() {
        return this.myArrayList.size();
    }

    public Object chosse(int index) {
        return (index < 0 || index >= this.length()) ? null : this.myArrayList
                .get(index);
    }

    public boolean chosseBoolean(int pIndex) {
        return this.chosseBoolean(pIndex, false);
    }

    public boolean chosseBoolean(int pIndex, boolean pDefaultvalue) {
        try {
            return this.getBoolean(pIndex);
        } catch (Exception e) {
            return pDefaultvalue;
        }
    }

    public double chouseDouble(int pIndex) {
        return this.chouseDouble(pIndex, Double.NaN);
    }

    public double chouseDouble(int pIndex, double pDefaultvalue) {
        final Number val = this.choosseNumber(pIndex, null);
        if (val == null) {
            return pDefaultvalue;
        }
        final double doubleValue = val.doubleValue();

        return doubleValue;
    }

    public float chooseFloat(int pIndex) {
        return this.chooseFloat(pIndex, Float.NaN);
    }

    public float chooseFloat(int pIndex, float pDefaultValue) {
        final Number val = this.choosseNumber(pIndex, null);
        if (val == null) {
            return pDefaultValue;
        }
        final float floatValue = val.floatValue();
        return floatValue;
    }

    public int choosseInt(int pIndex) {
        return this.choosseInt(pIndex, 0);
    }

    public int choosseInt(int pIndex, int pDefaultValue) {
        final Number val = this.choosseNumber(pIndex, null);
        if (val == null) {
            return pDefaultValue;
        }
        return val.intValue();
    }

    public BigInteger chooseBigInteger(int index, BigInteger defaultValue) {
        Object val = this.chosse(index);
        return JSONObject.objectToBigInteger(val, defaultValue);
    }

    public BigDecimal chooseBigDecimal(int index, BigDecimal defaultValue) {
        Object val = this.chosse(index);
        return JSONObject.objectToBigDecimal(val, defaultValue);
    }

    public JSONArray chooseJSONArray(int index) {
        Object o = this.chosse(index);
        return o instanceof JSONArray ? (JSONArray) o : null;
    }

    public JSONObject chooseJSONObject(int index) {
        Object o = this.chosse(index);
        return o instanceof JSONObject ? (JSONObject) o : null;
    }

    public long chooseLong(int index) {
        return this.chooseLong(index, 0);
    }

    public long chooseLong(int index, long defaultValue) {
        final Number val = this.choosseNumber(index, null);
        if (val == null) {
            return defaultValue;
        }
        return val.longValue();
    }

    public Number choosseNumber(int index) {
        return this.choosseNumber(index, null);
    }

    public Number choosseNumber(int index, Number defaultValue) {
        Object val = this.chosse(index);
        if (JSONObject.NULL.equals(val)) {
            return defaultValue;
        }
        if (val instanceof Number) {
            return (Number) val;
        }

        if (val instanceof String) {
            try {
                return JSONObject.stringToNumber((String) val);
            } catch (Exception e) {
                return defaultValue;
            }
        }
        return defaultValue;
    }

    public String chooseString(int index) {
        return this.chooseString(index, "");
    }

    public String chooseString(int index, String defaultValue) {
        Object object = this.chosse(index);
        return JSONObject.NULL.equals(object) ? defaultValue : object
                .toString();
    }

    public JSONArray put(boolean value) {
        return this.put(value ? Boolean.TRUE : Boolean.FALSE);
    }

    public JSONArray put(Collection<?> value) {
        return this.put(new JSONArray(value));
    }

    public JSONArray put(double value) throws JSONException {
        return this.put(Double.valueOf(value));
    }

    public JSONArray put(float value) throws JSONException {
        return this.put(Float.valueOf(value));
    }

    public JSONArray put(int value) {
        return this.put(Integer.valueOf(value));
    }

    public JSONArray put(long value) {
        return this.put(Long.valueOf(value));
    }

    public JSONArray put(Map<?, ?> value) {
        return this.put(new JSONObject(value));
    }

    public JSONArray put(Object value) {
        JSONObject.testValidity(value);
        this.myArrayList.add(value);
        return this;
    }

    public JSONArray put(int index, boolean value) throws JSONException {
        return this.put(index, value ? Boolean.TRUE : Boolean.FALSE);
    }

    public JSONArray put(int index, Collection<?> value) throws JSONException {
        return this.put(index, new JSONArray(value));
    }

    public JSONArray put(int index, double value) throws JSONException {
        return this.put(index, Double.valueOf(value));
    }

    public JSONArray put(int index, float value) throws JSONException {
        return this.put(index, Float.valueOf(value));
    }

    public JSONArray put(int index, int value) throws JSONException {
        return this.put(index, Integer.valueOf(value));
    }

    public JSONArray put(int index, long value) throws JSONException {
        return this.put(index, Long.valueOf(value));
    }

    public JSONArray put(int index, Map<?, ?> value) throws JSONException {
        this.put(index, new JSONObject(value));
        return this;
    }

    public JSONArray put(int index, Object value) throws JSONException {
        if (index < 0) {
            throw new JSONException("JSONArray[" + index + "] not found.");
        }
        if (index < this.length()) {
            JSONObject.testValidity(value);
            this.myArrayList.set(index, value);
            return this;
        }
        if (index == this.length()) {
            // simple append
            return this.put(value);
        }
        // if we are inserting past the length, we want to grow the array all at once
        // instead of incrementally.
        this.myArrayList.ensureCapacity(index + 1);
        while (index != this.length()) {
            // we don't need to test validity of NULL objects
            this.myArrayList.add(JSONObject.NULL);
        }
        return this.put(value);
    }


    public Object remove(int index) {
        return index >= 0 && index < this.length()
                ? this.myArrayList.remove(index)
                : null;
    }

    public boolean similar(Object other) {
        if (!(other instanceof JSONArray)) {
            return false;
        }
        int len = this.length();
        if (len != ((JSONArray) other).length()) {
            return false;
        }
        for (int i = 0; i < len; i += 1) {
            Object valueThis = this.myArrayList.get(i);
            Object valueOther = ((JSONArray) other).myArrayList.get(i);
            if (valueThis == valueOther) {
                continue;
            }
            if (valueThis == null) {
                return false;
            }
            if (valueThis instanceof JSONObject) {
                if (((JSONObject) valueThis).similar(valueOther)) {
                    return false;
                }
            } else if (valueThis instanceof JSONArray) {
                if (!((JSONArray) valueThis).similar(valueOther)) {
                    return false;
                }
            } else if (!valueThis.equals(valueOther)) {
                return false;
            }
        }
        return true;
    }

    public JSONObject toJSONObject(JSONArray names) throws JSONException {
        if (names == null || names.isEmpty() || this.isEmpty()) {
            return null;
        }
        JSONObject jo = new JSONObject(names.length());
        for (int i = 0; i < names.length(); i += 1) {
            jo.put(names.getString(i), this.chosse(i));
        }
        return jo;
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
        StringWriter sw = new StringWriter();
        synchronized (sw.getBuffer()) {
            return this.write(sw, indentFactor, 0).toString();
        }
    }

    public Writer write(Writer writer) throws JSONException {
        return this.write(writer, 0, 0);
    }

    public Writer write(Writer pWriter, int pIndentFactor, int pIndent)
            throws JSONException {
        try {
            boolean commanate = false;
            int length = this.length();
            pWriter.write('[');

            if (length == 1) {
                try {
                    JSONObject.writeValue(pWriter, this.myArrayList.get(0),
                            pIndentFactor, pIndent);
                } catch (Exception e) {
                    throw new JSONException("Unable to write JSONArray value at index: 0", e);
                }
            } else if (length != 0) {
                final int newindent = pIndent + pIndentFactor;

                for (int i = 0; i < length; i += 1) {
                    if (commanate) {
                        pWriter.write(',');
                    }
                    if (pIndentFactor > 0) {
                        pWriter.write('\n');
                    }
                    JSONObject.indent(pWriter, newindent);
                    try {
                        JSONObject.writeValue(pWriter, this.myArrayList.get(i),
                                pIndentFactor, newindent);
                    } catch (Exception e) {
                        throw new JSONException("Unable to write JSONArray value at index: " + i, e);
                    }
                    commanate = true;
                }
                if (pIndentFactor > 0) {
                    pWriter.write('\n');
                }
                JSONObject.indent(pWriter, pIndent);
            }
            pWriter.write(']');
            return pWriter;
        } catch (IOException e) {
            throw new JSONException(e);
        }
    }

    public boolean isEmpty() {
        return this.myArrayList.isEmpty();
    }

}
