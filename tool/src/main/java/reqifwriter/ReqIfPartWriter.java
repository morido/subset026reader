package reqifwriter;

import java.io.OutputStream;

interface ReqIfPartWriter {
    void writeToStream(final OutputStream outputStream);
}
