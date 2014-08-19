package com.prairie.eevernote.util;

import java.io.File;
import java.util.Iterator;
import java.util.List;

import javax.xml.parsers.ParserConfigurationException;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.custom.StyleRange;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.graphics.Point;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.handlers.HandlerUtil;
import org.w3c.dom.DOMException;

import com.prairie.eevernote.Constants;
import com.prairie.eevernote.enml.FontStyle;
import com.prairie.eevernote.enml.StyleText;

public class EclipseUtil implements Constants {

	public static List<File> getSelectedFiles(ExecutionEvent event) {
		ISelection selection = HandlerUtil.getActiveWorkbenchWindow(event).getActivePage().getSelection();

		final List<File> files = ListUtil.list();

		if (selection instanceof IStructuredSelection) {
			Iterator<?> iterator = ((StructuredSelection) selection).iterator();
			while (iterator.hasNext()) {
				IFile iFile;
				Object object = iterator.next();
				if (object instanceof IFile) {
					iFile = (IFile) object;
				} else if (object instanceof ICompilationUnit) {
					ICompilationUnit compilationUnit = (ICompilationUnit) object;
					IResource resource = compilationUnit.getResource();
					if (resource instanceof IFile) {
						iFile = (IFile) resource;
					} else {
						continue;
					}
				} else {
					continue;
				}
				File file = iFile.getLocation().makeAbsolute().toFile();
				files.add(file);
			}
		} else if (selection instanceof ITextSelection) {
			IEditorPart editorPart = HandlerUtil.getActiveEditor(event);
			IFile iFile = (IFile) ((editorPart.getEditorInput().getAdapter(IFile.class)));
			if (iFile != null) {// TODO iFile == null: how to handle this
								// case in XML file
				File file = iFile.getLocation().makeAbsolute().toFile();
				files.add(file);
			}
		}

		return files;
	}

	public static List<List<StyleText>> getStyleText(StyledText styledText) throws DOMException, ParserConfigurationException {
		Point selection = styledText.getSelection();
		String selectionText = styledText.getSelectionText();

		String face = StringUtil.EMPTY;
		int size = TEN;
		FontData[] fontDatas = styledText.getFont().getFontData();
		if (fontDatas != null && fontDatas.length > ZERO) {
			face = fontDatas[ZERO].getName();
			size = fontDatas[ZERO].getHeight();
		}

		String[] lines = StringUtils.splitByWholeSeparatorPreserveAllTokens(selectionText, StringUtil.CRLF);
		int count = ZERO;
		List<List<StyleText>> list = ListUtil.list();
		for (int i = ZERO; i < lines.length; i++) {
			int offset = selection.x + (count += (i <= ZERO ? ZERO : lines[i - ONE].length())) + (i * TWO);
			StyleRange[] ranges = styledText.getStyleRanges(offset, lines[i].length());
			List<StyleText> textRanges = parseLine(lines[i], ranges, offset, face, String.valueOf(size));
			list.add(textRanges);
		}
		return list;
	}

	// [PlainText][StyledText][PlainTex]
	private static List<StyleText> parseLine(String text, StyleRange[] styleRanges, int offset, String face, String size) {
		List<StyleText> textRanges = ListUtil.list();

		if (ArrayUtil.nullOrEmptyArray(styleRanges)) {
			StyleText textRange = new StyleText(text);
			textRanges.add(textRange);
			return textRanges;
		}

		int count = 0;
		for (int i = 0; i < styleRanges.length; i++) {
			int start = styleRanges[i].start - offset;

			// [PlainText] - Part1
			String part = text.substring(count, start);
			if (!StringUtil.nullOrEmptyString(part)) {
				StyleText textRange = new StyleText(part);
				textRanges.add(textRange);
				count += part.length();
			}

			// // [StyledText]
			part = text.substring(start, start + styleRanges[i].length);
			Color foreground = styleRanges[i].foreground != null ? styleRanges[i].foreground : ColorUtil.SWT_DEFAULT_COLOR;
			StyleText textRange = new StyleText(part, face, ColorUtil.toHexCode(foreground.getRed(), foreground.getGreen(), foreground.getBlue()), size, FontStyle.forNumber(styleRanges[i].fontStyle));
			textRanges.add(textRange);
			count += part.length();
		}
		// [PlainText] - Part2
		String part = text.substring(count);
		if (!StringUtil.nullOrEmptyString(part)) {
			StyleText textRange = new StyleText(part);
			textRanges.add(textRange);
		}

		return textRanges;
	}

}