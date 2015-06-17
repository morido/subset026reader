package docreader.range.paragraph.characterRun;

import helper.Destructible;

import org.apache.poi.hwpf.usermodel.CharacterRun;

interface CharacterRunReaderGeneric extends Destructible {
    static final char UNICODECHAR_VERTICALTAB = '\u000B';
    void read(final CharacterRun characterRun);
}
