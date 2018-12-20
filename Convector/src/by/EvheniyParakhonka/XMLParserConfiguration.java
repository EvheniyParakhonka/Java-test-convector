package by.EvheniyParakhonka;

public class XMLParserConfiguration {
    public static final XMLParserConfiguration ORIGINAL = new XMLParserConfiguration();

    public static final XMLParserConfiguration KEEP_STRINGS = new XMLParserConfiguration(true);

    public final boolean keepStrings;

    public final String cDataTagName;

    public XMLParserConfiguration () {
          this(false, "content");
    }

    public XMLParserConfiguration (final boolean keepStrings) {
          this(keepStrings, "content");
    }

    public XMLParserConfiguration (final String cDataTagName) {
          this(false, cDataTagName);
    }

    public XMLParserConfiguration (final boolean keepStrings, final String cDataTagName) {
          this.keepStrings = keepStrings;
          this.cDataTagName = cDataTagName;
    }
}
