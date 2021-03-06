/* Copyright 2010-2015 Norconex Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.norconex.collector.http.sitemap.impl;

import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.util.Objects;

import javax.xml.stream.XMLStreamException;

import org.apache.commons.configuration.XMLConfiguration;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import com.norconex.collector.http.crawler.HttpCrawlerConfig;
import com.norconex.collector.http.sitemap.ISitemapResolver;
import com.norconex.collector.http.sitemap.ISitemapResolverFactory;
import com.norconex.commons.lang.config.ConfigurationUtil;
import com.norconex.commons.lang.config.IXMLConfigurable;
import com.norconex.commons.lang.xml.EnhancedXMLStreamWriter;

/**
 * Factory used to created {@link StandardSitemapResolver} instances.
 * Refer to {@link StandardSitemapResolver} for resolution logic.
 * @author Pascal Essiembre
 *
 * <p>
 * XML configuration usage:
 * </p>
 * <pre>
 *  &lt;sitemapResolverFactory ignore="[false|true]" lenient="[false|true]" 
 *        tempDir="(where to store temp files)"
 *     class="com.norconex.collector.http.sitemap.impl.StandardSitemapResolverFactory"&gt;
 *     &lt;path&gt;(optional path relative to URL root for a sitemap)&lt;/path&gt;
 *     (... repeat path tag as needed ...)
 *  &lt;/sitemapResolverFactory&gt;
 * </pre>
 * @see StandardSitemapResolver
 */
public class StandardSitemapResolverFactory 
        implements ISitemapResolverFactory, IXMLConfigurable {

    private static final Logger LOG = LogManager.getLogger(
            StandardSitemapResolverFactory.class);

    private File tempDir;
    private String[] sitemapPaths;
    private boolean lenient;
    
    @Override
    public ISitemapResolver createSitemapResolver(
            HttpCrawlerConfig config, boolean resume) {
  
        File resolvedTempDir = tempDir;
        if (resolvedTempDir == null) {
            resolvedTempDir = config.getWorkDir();
        }
        if (resolvedTempDir == null) {
            resolvedTempDir = FileUtils.getTempDirectory();
        }
        StandardSitemapResolver sr = new StandardSitemapResolver(
                resolvedTempDir, new SitemapStore(config, resume));
        sr.setLenient(lenient);
        sr.setSitemapPaths(sitemapPaths);
        return sr;
    }

    /**
     * Gets the URL paths, relative to the URL root, from which to try 
     * locate and resolve sitemaps. Default paths are 
     * "/sitemap.xml" and "/sitemap-index.xml".
     * @return sitemap paths.
     * @since 2.3.0
     */
    public String[] getSitemapPaths() {
        return sitemapPaths;
    }
    /**
     * Sets the URL paths, relative to the URL root, from which to try 
     * locate and resolve sitemaps.
     * @param sitemapPaths sitemap paths.
     * @since 2.3.0
     */
    public void setSitemapPaths(String... sitemapPaths) {
        this.sitemapPaths = sitemapPaths;
    }
    
    /**
     * Get sitemap locations.
     * @return sitemap locations
     * @deprecated Since 2.3.0, use {@link HttpCrawlerConfig#getStartSitemapURLs()}
     */
    @Deprecated
    public String[] getSitemapLocations() {
        LOG.warn("Since 2.3.0, calling StandardSitemapResolver"
                + "#getSitemapLocation() has no effect. "
                + "Use HttpCrawlerConfig#getSitemaps() instead.");
        return null;
    }
    /**
     * Set sitemap locations.
     * @param sitemapLocations sitemap locations
     * @deprecated Since 2.3.0, use 
     *             {@link HttpCrawlerConfig#setStartSitemapURLs(String[])}
     */
    public void setSitemapLocations(String... sitemapLocations) {
        LOG.warn("Since 2.3.0, calling StandardSitemapResolver"
                + "#setSitemapLocation(String...) has no effect. "
                + "Use HttpCrawlerConfig#setSitemaps(String[] ...) instead.");
    }
    
    public boolean isLenient() {
        return lenient;
    }
    public void setLenient(boolean lenient) {
        this.lenient = lenient;
    }

    /**
     * Gets the directory where sitemap files are temporary stored
     * before they are parsed.  When <code>null</code> (default), temporary
     * files are created directly under {@link HttpCrawlerConfig#getWorkDir()}.
     * the crawler working directory is also undefined, it will use the 
     * system temporary directory, as returned by 
     * {@link FileUtils#getTempDirectory()}.
     * @return directory where temporary files are written
     * @since 2.3.0
     */
    public File getTempDir() {
        return tempDir;
    }
    /**
     * Sets the temporary directory where sitemap files are temporary stored
     * before they are parsed.  
     * @param tempDir directory where temporary files are written
     * @since 2.3.0
     */
    public void setTempDir(File tempDir) {
        this.tempDir = tempDir;
    }

    @Override
    public void loadFromXML(Reader in) throws IOException {
        XMLConfiguration xml = ConfigurationUtil.newXMLConfiguration(in);
        String tempPath = xml.getString(
                "tempDir", Objects.toString(getTempDir(), null));
        if (tempPath != null) {
            setTempDir(new File(tempPath));
        }
        setLenient(xml.getBoolean("[@lenient]", false));
        setSitemapPaths(xml.getList(
                "path").toArray(ArrayUtils.EMPTY_STRING_ARRAY));
        
        if (!xml.getList("location").isEmpty()) {
            LOG.warn("Since 2.3.0, the location tag is no longer supported. "
                    + "Use <sitemap> under <startURLs> for an equivalent.");
        }
    }

    @Override
    public void saveToXML(Writer out) throws IOException {
        try {
            EnhancedXMLStreamWriter writer = new EnhancedXMLStreamWriter(out);
            writer.writeStartElement("sitemapResolverFactory");
            writer.writeAttribute("class", getClass().getCanonicalName());
            writer.writeAttribute("lenient", Boolean.toString(lenient));
            writer.writeElementString(
                    "tempDir", Objects.toString(getTempDir(), null));
            if (sitemapPaths != null) {
                for (String path : sitemapPaths) {
                    writer.writeStartElement("path");
                    writer.writeCharacters(path);
                    writer.writeEndElement();
                }
            }
            writer.writeEndElement();
            writer.flush();
            writer.close();
        } catch (XMLStreamException e) {
            throw new IOException("Cannot save as XML.", e);
        }
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this, ToStringStyle.SHORT_PREFIX_STYLE)
                .append("sitemapPaths", sitemapPaths)
                .append("lenient", lenient)
                .append("tempDir", tempDir)
                .toString();
    }

    @Override
    public boolean equals(final Object other) {
        if (!(other instanceof StandardSitemapResolverFactory)) {
            return false;
        }
        StandardSitemapResolverFactory castOther = 
                (StandardSitemapResolverFactory) other;
        return new EqualsBuilder()
                .append(sitemapPaths, castOther.sitemapPaths)
                .append(lenient, castOther.lenient)
                .append(tempDir, castOther.tempDir)
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder()
                .append(sitemapPaths)
                .append(lenient)
                .append(tempDir)
                .toHashCode();
    }
    
}
