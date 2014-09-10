package com.prairie.eevernote.client;

import java.io.File;
import java.util.List;

import com.prairie.eevernote.enml.StyleText;

public interface ENNote extends ENObject {

    public ENObject getNotebook();

    public void setNotebook(ENObject notebook);

    public List<List<StyleText>> getContent();

    public void setContent(List<List<StyleText>> content);

    public List<File> getAttachments();

    public void setAttachments(List<File> files);

    public List<String> getTags();

    public abstract void setTags(List<String> tags);

    public abstract String getComments();

    public abstract void setComments(String comments);

}
