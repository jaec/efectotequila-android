/*
 * efectotequila is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * efectotequila is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with efectotequila.  If not, see <http://www.gnu.org/licenses/>.
 */

package net.efectotequila.android.feedgoal;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.TimeZone;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import net.efectotequila.android.feedgoal.common.Enclosure;
import net.efectotequila.android.feedgoal.common.Feed;
import net.efectotequila.android.feedgoal.common.Item;
import net.efectotequila.android.feedgoal.storage.SharedPreferencesHelper;
import net.efectotequila.android.feedgoal.util.URLReader;

import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;

import android.content.Context;
import android.text.Html;
import android.text.SpannableStringBuilder;
import android.text.Spanned;

import org.apache.commons.lang3.*;

public class FeedHandler extends DefaultHandler {

	private static final String LOG_TAG = "FeedHandler";

	private Feed mFeed;
	private Item mItem;
	private Enclosure mEnclosure;

	// RSS Date Formats:
	// RFC822 Time Zone: EEE, dd MMM yyyy HH:mm:ss Z
	// General Time Zone: EEE, dd MMM yyyy HH:mm:ss z
	// Atom Date Formats: ISO8601 variants
	// Common Atom Date Formats: yyyy-MM-dd'T'HH:mm:ssZ
	// yyyy-MM-dd'T'HH:mm:ssz
	// Google Reader Shared Items Atom Date Format: yyyy-MM-dd'T'HH:mm:ss'Z'
	// Android Developers Blog Atom Date Format: yyyy-MM-dd'T'HH:mm:ss.SSSZ
	private static final String DATE_FORMATS[] = {
			"EEE, dd MMM yyyy HH:mm:ss Z", "EEE, dd MMM yyyy HH:mm:ss z",
			"yyyy-MM-dd'T'HH:mm:ssz", "yyyy-MM-dd'T'HH:mm:ssZ",
			"yyyy-MM-dd'T'HH:mm:ss'Z'", "yyyy-MM-dd'T'HH:mm:ss.SSSZ" };
	private SimpleDateFormat mSimpleDateFormats[] = new SimpleDateFormat[DATE_FORMATS.length];

	// Allowed Namespaces
	private static final Set<String> NAMESPACES = new HashSet<String>(
			Arrays.asList(new String[] { "",
					"http://purl.org/rss/1.0/modules/content/",
					"http://www.w3.org/2005/Atom", "http://purl.org/rss/1.0/",
					"http://purl.org/dc/elements/1.1/" }));

	private boolean isType = false;
	private boolean isFeed = false;
	private boolean isItem = false;
	private boolean isTitle = false;
	private boolean isLink = false;
	private boolean isPubdate = false;
	private boolean isGuid = false;
	private boolean isDescription = false;
	private boolean isContent = false;
	private boolean isSource = false; // used to escape the <source> element in
										// Atom format
	private boolean isEnclosure = false;

	private String mHrefAttribute; // href attribute from link element in Atom
									// format and enclosures for Atom and RSS
									// formats
	private String mMimeAttribute; // Enclosure MIME type attribute from link
									// element for RSS and Atom formats
	private int maxItems = 0;
	private int mNbrItems = 0;
	private StringBuffer mSb;

	public FeedHandler(Context ctx) {
		maxItems = SharedPreferencesHelper.getPrefMaxItems(ctx);

		for (int i = 0; i < DATE_FORMATS.length; i++) {
			// mSimpleDateFormats[i] = new SimpleDateFormat(DATE_FORMATS[i]);
			// If Locale.US parameter is not set, a parsing error occurs on
			// published date when device locale is set to a language other than
			// English.
			// Because published date format in feed is used to set to English.
			mSimpleDateFormats[i] = new SimpleDateFormat(DATE_FORMATS[i],
					Locale.US);
			mSimpleDateFormats[i].setTimeZone(TimeZone.getTimeZone("GMT"));
		}
	}

	public void startDocument() throws SAXException {
		mFeed = new Feed();
	}

	public void endDocument() throws SAXException {
		Date now = new Date();
		// Date now = Calendar.getInstance().getTime();
		mFeed.setRefresh(now);
	}

	public void startElement(String uri, String localName, String qName,
			Attributes attributes) throws SAXException {
		// Only consider elements from allowed third-party namespaces
		if (NAMESPACES.contains(uri)) {
			mSb = new StringBuffer();
			String value = localName.trim();

			if (value.equalsIgnoreCase("rss") || value.equalsIgnoreCase("rdf")) {
				isType = true;
			} else if (value.equalsIgnoreCase("feed")) {
				isType = true;
				isFeed = true;
			} else if (value.equalsIgnoreCase("channel")) {
				isFeed = true;
			} else if (value.equalsIgnoreCase("item")
					|| value.equalsIgnoreCase("entry")) {
				mItem = new Item();
				isItem = true;
				mNbrItems++;
			} else if (value.equalsIgnoreCase("title"))
				isTitle = true;
			else if (value.equalsIgnoreCase("link")) {
				// Get attributes from link element for Atom format
				if (attributes != null) {
					// Enclosure for Atom format
					if (attributes.getValue("rel") != null
							&& attributes.getValue("rel").equalsIgnoreCase(
									"enclosure")) {
						mEnclosure = new Enclosure();
						mMimeAttribute = attributes.getValue("type");
						isEnclosure = true;
					}
					mHrefAttribute = attributes.getValue("href");
				}
				isLink = true;
			} else if (value.equalsIgnoreCase("pubDate")
					|| value.equalsIgnoreCase("published")
					|| value.equalsIgnoreCase("date"))
				isPubdate = true;
			else if (value.equalsIgnoreCase("guid")
					|| value.equalsIgnoreCase("id"))
				isGuid = true;
			else if (value.equalsIgnoreCase("description")
					|| value.equalsIgnoreCase("summary"))
				isDescription = true;
			else if (value.equalsIgnoreCase("encoded")
					|| value.equalsIgnoreCase("content"))
				isContent = true;
			else if (value.equalsIgnoreCase("source"))
				isSource = true;
			else if (value.equalsIgnoreCase("enclosure")) {
				// Enclosure for RSS format
				if (attributes != null) {
					mEnclosure = new Enclosure();
					mMimeAttribute = attributes.getValue("type");
					mHrefAttribute = attributes.getValue("url");
					isEnclosure = true;
				}
			}
		}
	}

	public void endElement(String uri, String localName, String qName)
			throws SAXException {
		// Only consider elements from allowed third-party namespaces
		if (NAMESPACES.contains(uri)) {
			String value = localName.trim();

			if (value.equalsIgnoreCase("rss")) {
				mFeed.setType(Feed.TYPE_RSS);
				isType = false;
			} else if (value.equalsIgnoreCase("feed")) {
				mFeed.setType(Feed.TYPE_ATOM);
				isType = false;
				isFeed = false;
			} else if (value.equalsIgnoreCase("RDF")) {
				mFeed.setType(Feed.TYPE_RDF);
				isType = false;
			} else if (value.equalsIgnoreCase("channel")) {
				isFeed = false;
			} else if (value.equalsIgnoreCase("item")
					|| value.equalsIgnoreCase("entry")) {
				if (mNbrItems <= maxItems) {
					if (mItem.getGuid() == null)
						mItem.setGuid(mItem.getLink().toString());
					mFeed.addItem(mItem);
				}
				isItem = false;
			} else if (value.equalsIgnoreCase("title") && !isSource) {
				if (isItem)
					mItem.setTitle(Html.fromHtml(mSb.toString().trim())
							.toString());
				else if (isFeed)
					mFeed.setTitle(Html.fromHtml(mSb.toString().trim())
							.toString());
				isTitle = false;
			} else if (value.equalsIgnoreCase("link") && !isSource) {
				if (isItem) {
					try {
						if (isEnclosure) {
							// Enclosure for Atom format
							mEnclosure.setMime(mMimeAttribute);
							mEnclosure.setURL(new URL(mHrefAttribute));
							mItem.addEnclosure(mEnclosure);
							mMimeAttribute = null;
							isEnclosure = false;
						} else if (mHrefAttribute != null)
							mItem.setLink(new URL(mHrefAttribute));
						else
							mItem.setLink(new URL(mSb.toString().trim()));
					} catch (MalformedURLException mue) {
						throw new SAXException(mue);
					}
				} else if (isFeed && mFeed.getHomePage() == null) {
					try {
						if (mSb != null && mSb.toString() != "") // RSS
							mFeed.setHomePage(new URL(mSb.toString().trim()));
						else if (mMimeAttribute == "text/html") // Atom
							mFeed.setHomePage(new URL(mHrefAttribute));
					} catch (MalformedURLException mue) {
						throw new SAXException(mue);
					}
				}
				mHrefAttribute = null;
				isLink = false;
			} else if (value.equalsIgnoreCase("pubDate")
					|| value.equalsIgnoreCase("published")
					|| value.equalsIgnoreCase("date")) {
				if (isItem) {
					for (int i = 0; i < DATE_FORMATS.length; i++) {
						try {
							// String pattern =
							// mSimpleDateFormats[i].toPattern();
							mItem.setPubdate(mSimpleDateFormats[i].parse(mSb
									.toString().trim()));
							break;
						} catch (ParseException pe) {
							if (i == DATE_FORMATS.length - 1) {
								throw new SAXException(pe);
							}
						}
					}
				}
				isPubdate = false;
			} else if ((value.equalsIgnoreCase("guid") || value
					.equalsIgnoreCase("id")) && !isSource) {
				if (isItem)
					mItem.setGuid(mSb.toString().trim());
				isGuid = false;
			} else if (value.equalsIgnoreCase("description")
					|| value.equalsIgnoreCase("summary")) {
				if (isItem) {
					// mItem.setContent(Html.fromHtml(mSb.toString().trim()).toString());
					/*mItem.setContent(removeContentSpanObjects(mSb).toString()
							.trim() + System.getProperty("line.separator"));*/ // JAEC
					/*String imgTagsReplaced = StringUtils.replace(mSb.toString().trim(), "<a ", "<espan ");
					imgTagsReplaced = StringUtils.replace(imgTagsReplaced, "</a>", "</espan>");
					mItem.setContent(imgTagsReplaced);*/
					mItem.setContent(mSb.toString().trim());
					
					/** meneos **/
					mItem.setVotes(getMeneos(mItem.getContent()));
				}
				isDescription = false;
			} else if (value.equalsIgnoreCase("encoded")
					|| value.equalsIgnoreCase("content")) {
				if (isItem) {
					// mItem.setContent(Html.fromHtml(mSb.toString().trim()).toString());
					/*mItem.setContent(removeContentSpanObjects(mSb).toString()
							.trim() + System.getProperty("line.separator"));*/ // JAEC
					/*String imgTagsReplaced = StringUtils.replace(mSb.toString().trim(), "<a ", "<espan ");
					imgTagsReplaced = StringUtils.replace(imgTagsReplaced, "</a>", "</espan>");
					mItem.setContent(imgTagsReplaced);*/
					mItem.setContent(mSb.toString().trim());
					
					/** meneos **/
					mItem.setVotes(getMeneos(mItem.getContent()));
				}
				isContent = false;
			} else if (value.equalsIgnoreCase("source"))
				isSource = false;
			else if (value.equalsIgnoreCase("enclosure")) {
				if (isItem) {
					try {
						// Enclosure for RSS format
						mEnclosure.setMime(mMimeAttribute);
						mEnclosure.setURL(new URL(mHrefAttribute));
						mItem.addEnclosure(mEnclosure);
						mMimeAttribute = null;
						mHrefAttribute = null;
					} catch (MalformedURLException mue) {
						throw new SAXException(mue);
					}
				}
				isEnclosure = false;
			}
		}
	}
	
	private int getMeneos(String content) {
		String id = "";
		int s = StringUtils.indexOf(content, "id=");
		id = StringUtils.substring(content, s);
		int e = StringUtils.indexOf(id, "\"");
		id = StringUtils.substring(id, 3, e).trim();
		//String votes = "";
		
		try {
			String meneos = new URLReader("https://www.efectotequila.com/backend/meneos.php?id=" + id).read().toString();
			if(!meneos.equals("")) {
				int v = StringUtils.countMatches(meneos, "href=\"/user/");
				//votes = String.valueOf(v);
				return v;
			}
		} catch (MalformedURLException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		
		return 0;
	}

	public void characters(char[] ch, int start, int length)
			throws SAXException {
		if (isType || isTitle || isLink || isPubdate || isGuid || isDescription
				|| isContent)
			mSb.append(new String(ch, start, length));
	}

	public Feed handleFeed(URL url) throws IOException, SAXException,
			ParserConfigurationException {
		InputSource is = new InputSource(url.openStream());
		System.out.println(" ============ " + url.getPath());
		is.setEncoding("UTF-8");
		getParser().parse(is);
		// Reordering the list of items, first item parsed (most recent) -> last
		// item in the list
		Collections.reverse(mFeed.getItems());
		mFeed.setURL(url);
		if (mFeed.getHomePage() == null)
			mFeed.setHomePage(url);
		return mFeed;
	}

	private XMLReader getParser() throws SAXException,
			ParserConfigurationException {
		SAXParserFactory spf = SAXParserFactory.newInstance();
		SAXParser sp = spf.newSAXParser();
		XMLReader xr = sp.getXMLReader();
		xr.setContentHandler(this);
		return xr;
	}

	private Spanned removeContentSpanObjects(StringBuffer sb) {
		SpannableStringBuilder spannedStr = (SpannableStringBuilder) Html
				.fromHtml(sb.toString().trim());
		Object[] spannedObjects = spannedStr.getSpans(0, spannedStr.length(),
				Object.class);
		for (int i = 0; i < spannedObjects.length; i++) {
			// if (!(spannedObjects[i] instanceof URLSpan) &&
			// !(spannedObjects[i] instanceof StyleSpan))
			
			/*if (spannedObjects[i] instanceof ImageSpan)
				spannedStr.replace(spannedStr.getSpanStart(spannedObjects[i]),
						spannedStr.getSpanEnd(spannedObjects[i]), "");*/ // JAEC
			
			// spannedStr.removeSpan(spannedObjects[i]);
		}
		// spannedStr.clearSpans();
		return spannedStr;
	}
}
