package com.earnix.webk.dom.nodes;

import com.earnix.webk.dom.UncheckedIOException;

import java.io.IOException;

/**
 * A Character Data node, to support CDATA sections.
 */
public class CDataNodeModel extends TextNodeModel {
    public CDataNodeModel(String text) {
        super(text);
    }

    @Override
    public String nodeName() {
        return "#cdata";
    }

    /**
     * Get the unencoded, <b>non-normalized</b> text content of this CDataNode.
     *
     * @return unencoded, non-normalized text
     */
    @Override
    public String text() {
        return getWholeText();
    }

    @Override
    void outerHtmlHead(Appendable accum, int depth, DocumentModel.OutputSettings out) throws IOException {
        accum
                .append("<![CDATA[")
                .append(getWholeText());
    }

    @Override
    void outerHtmlTail(Appendable accum, int depth, DocumentModel.OutputSettings out) {
        try {
            accum.append("]]>");
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}