/*
 * #%L
 * Alfresco Repository
 * %%
 * Copyright (C) 2005 - 2018 Alfresco Software Limited
 * %%
 * This file is part of the Alfresco software.
 * If the software was purchased under a paid Alfresco license, the terms of
 * the paid license agreement will prevail.  Otherwise, the software is
 * provided under the following open source license terms:
 *
 * Alfresco is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Alfresco is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Alfresco. If not, see <http://www.gnu.org/licenses/>.
 * #L%
 */
package org.alfresco.repo.rendition2;

import org.alfresco.repo.content.transform.magick.ImageResizeOptions;
import org.alfresco.repo.content.transform.magick.ImageTransformationOptions;
import org.alfresco.repo.content.transform.swf.SWFTransformationOptions;
import org.alfresco.service.cmr.repository.CropSourceOptions;
import org.alfresco.service.cmr.repository.PagedSourceOptions;
import org.alfresco.service.cmr.repository.TemporalSourceOptions;
import org.alfresco.service.cmr.repository.TransformationOptionLimits;
import org.alfresco.service.cmr.repository.TransformationOptions;
import org.alfresco.service.cmr.repository.TransformationSourceOptions;
import org.alfresco.util.PropertyCheck;
import org.springframework.beans.factory.InitializingBean;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringJoiner;

import static org.alfresco.repo.rendition2.RenditionDefinition2.ALLOW_ENLARGEMENT;
import static org.alfresco.repo.rendition2.RenditionDefinition2.AUTO_ORIENT;
import static org.alfresco.repo.rendition2.RenditionDefinition2.CROP_GRAVITY;
import static org.alfresco.repo.rendition2.RenditionDefinition2.CROP_HEIGHT;
import static org.alfresco.repo.rendition2.RenditionDefinition2.CROP_PERCENTAGE;
import static org.alfresco.repo.rendition2.RenditionDefinition2.CROP_WIDTH;
import static org.alfresco.repo.rendition2.RenditionDefinition2.CROP_X_OFFSET;
import static org.alfresco.repo.rendition2.RenditionDefinition2.CROP_Y_OFFSET;
import static org.alfresco.repo.rendition2.RenditionDefinition2.DURATION;
import static org.alfresco.repo.rendition2.RenditionDefinition2.END_PAGE;
import static org.alfresco.repo.rendition2.RenditionDefinition2.FLASH_VERSION;
import static org.alfresco.repo.rendition2.RenditionDefinition2.HEIGHT;
import static org.alfresco.repo.rendition2.RenditionDefinition2.INCLUDE_CONTENTS;
import static org.alfresco.repo.rendition2.RenditionDefinition2.MAINTAIN_ASPECT_RATIO;
import static org.alfresco.repo.rendition2.RenditionDefinition2.MAX_SOURCE_SIZE_K_BYTES;
import static org.alfresco.repo.rendition2.RenditionDefinition2.OFFSET;
import static org.alfresco.repo.rendition2.RenditionDefinition2.PAGE;
import static org.alfresco.repo.rendition2.RenditionDefinition2.RESIZE_HEIGHT;
import static org.alfresco.repo.rendition2.RenditionDefinition2.RESIZE_PERCENTAGE;
import static org.alfresco.repo.rendition2.RenditionDefinition2.RESIZE_WIDTH;
import static org.alfresco.repo.rendition2.RenditionDefinition2.START_PAGE;
import static org.alfresco.repo.rendition2.RenditionDefinition2.THUMBNAIL;
import static org.alfresco.repo.rendition2.RenditionDefinition2.TIMEOUT;
import static org.alfresco.repo.rendition2.RenditionDefinition2.WIDTH;
import static org.springframework.util.CollectionUtils.containsAny;

/**
 * @deprecated converts the new flat name value pair transformer options to the depreacted TransformationOptions.
 *
 * @author adavis
 */
@Deprecated
public class TransformationOptionsConverter implements InitializingBean
{
    private static Set<String> PAGED_OPTIONS = new HashSet<>(Arrays.asList(new String[]
            {
                    PAGE, START_PAGE, END_PAGE
            }));

    private static Set<String> CROP_OPTIONS = new HashSet<>(Arrays.asList(new String[]
            {
                    CROP_GRAVITY, CROP_WIDTH, CROP_HEIGHT, CROP_PERCENTAGE, CROP_X_OFFSET, CROP_Y_OFFSET
            }));

    private static Set<String> TEMPORAL_OPTIONS = new HashSet<>(Arrays.asList(new String[]
            {
                    OFFSET, DURATION
            }));

    private static Set<String> RESIZE_OPTIONS = new HashSet<>(Arrays.asList(new String[]
            {
                    WIDTH, HEIGHT,
                    THUMBNAIL, RESIZE_WIDTH, RESIZE_HEIGHT, RESIZE_PERCENTAGE,
                    ALLOW_ENLARGEMENT, MAINTAIN_ASPECT_RATIO
            }));

    private static Set<String> IMAGE_OPTIONS = new HashSet<>();
    static
    {
        IMAGE_OPTIONS.addAll(PAGED_OPTIONS);
        IMAGE_OPTIONS.addAll(CROP_OPTIONS);
        IMAGE_OPTIONS.addAll(TEMPORAL_OPTIONS);
        IMAGE_OPTIONS.addAll(RESIZE_OPTIONS);
    }

    private static Set<String> PDF_OPTIONS = new HashSet<>(Arrays.asList(new String[]
            {
                    PAGE, WIDTH, HEIGHT
            }));

    private static Set<String> FLASH_OPTIONS = new HashSet<>(Arrays.asList(new String[]
            {
                    FLASH_VERSION
            }));

    private static Set<String> LIMIT_OPTIONS = new HashSet<>(Arrays.asList(new String[]
            {
                    TIMEOUT, MAX_SOURCE_SIZE_K_BYTES
            }));

    private interface Setter
    {
        void set(String s);
    }

    // The default valued in the old TransformationOptionsLimits
    private long maxSourceSizeKBytes;
    private long readLimitTimeMs;
    private long readLimitKBytes;
    private int pageLimit;
    private int maxPages;

    public void setMaxSourceSizeKBytes(String maxSourceSizeKBytes)
    {
        this.maxSourceSizeKBytes = Long.parseLong(maxSourceSizeKBytes);;
    }

    public void setReadLimitTimeMs(String readLimitTimeMs)
    {
        this.readLimitTimeMs = Long.parseLong(readLimitTimeMs);
    }

    public void setReadLimitKBytes(String readLimitKBytes)
    {
        this.readLimitKBytes = Long.parseLong(readLimitKBytes);
    }

    public void setPageLimit(String pageLimit)
    {
        this.pageLimit = Integer.parseInt(pageLimit);
    }

    public void setMaxPages(String maxPages)
    {
        this.maxPages = Integer.parseInt(maxPages);
    }

    @Override
    public void afterPropertiesSet()
    {
        PropertyCheck.mandatory(this, "maxSourceSizeKBytes", maxSourceSizeKBytes);
        PropertyCheck.mandatory(this, "readLimitTimeMs", readLimitTimeMs);
        PropertyCheck.mandatory(this, "readLimitKBytes", readLimitKBytes);
        PropertyCheck.mandatory(this, "pageLimit", pageLimit);
        PropertyCheck.mandatory(this, "maxPages", maxPages);
    }

    /**
     * @deprecated as we do not plan to use TransformationOptions moving forwards as local transformations will also
     * use the same options as the Transform Service.
     */
    @Deprecated
    TransformationOptions getTransformationOptions(String renditionName, Map<String, String> options)
    {
        TransformationOptions transformationOptions = null;
        Set<String> optionNames = options.keySet();

        // The "pdf" rendition is special as it was incorrectly set up as an SWFTransformationOptions in 6.0
        // It should have been simply a TransformationOptions.
        boolean isPdfRendition = "pdf".equals(renditionName);

        Set<String> subclassOptionNames = new HashSet<>(optionNames);
        subclassOptionNames.removeAll(LIMIT_OPTIONS);
        subclassOptionNames.remove(INCLUDE_CONTENTS);
        boolean hasOptions = !subclassOptionNames.isEmpty();
        if (isPdfRendition || hasOptions)
        {
            if (isPdfRendition || FLASH_OPTIONS.containsAll(subclassOptionNames))
            {
                SWFTransformationOptions opts = new SWFTransformationOptions();
                transformationOptions = opts;
                opts.setFlashVersion(isPdfRendition ? "9" : options.get(FLASH_VERSION));
            }
            else if (IMAGE_OPTIONS.containsAll(subclassOptionNames) ||  PDF_OPTIONS.containsAll(subclassOptionNames))
            {
                ImageTransformationOptions opts = new ImageTransformationOptions();
                transformationOptions = opts;

                if (containsAny(subclassOptionNames, RESIZE_OPTIONS))
                {
                    ImageResizeOptions imageResizeOptions = new ImageResizeOptions();
                    opts.setResizeOptions(imageResizeOptions);
                    // PDF
                    ifSet(options, WIDTH, (v) -> imageResizeOptions.setWidth(Integer.parseInt(v)));
                    ifSet(options, HEIGHT, (v) -> imageResizeOptions.setHeight(Integer.parseInt(v)));
                    // ImageMagick
                    ifSet(options, RESIZE_WIDTH, (v) -> imageResizeOptions.setWidth(Integer.parseInt(v)));
                    ifSet(options, RESIZE_HEIGHT, (v) -> imageResizeOptions.setHeight(Integer.parseInt(v)));
                    ifSet(options, THUMBNAIL, (v) ->imageResizeOptions.setResizeToThumbnail(Boolean.parseBoolean(v)));
                    ifSet(options, RESIZE_PERCENTAGE, (v) ->imageResizeOptions.setPercentResize(Boolean.parseBoolean(v)));
                    ifSet(options, ALLOW_ENLARGEMENT, (v) ->imageResizeOptions.setAllowEnlargement(Boolean.parseBoolean(v)));
                    ifSet(options, MAINTAIN_ASPECT_RATIO, (v) ->imageResizeOptions.setMaintainAspectRatio(Boolean.parseBoolean(v)));
                }

                ifSet(options, AUTO_ORIENT, (v) ->opts.setAutoOrient(Boolean.parseBoolean(v)));

                boolean containsPaged = containsAny(subclassOptionNames, PAGED_OPTIONS);
                boolean containsCrop = containsAny(subclassOptionNames, CROP_OPTIONS);
                boolean containsTemporal = containsAny(subclassOptionNames, TEMPORAL_OPTIONS);
                if (containsPaged || containsCrop || containsTemporal)
                {
                    List<TransformationSourceOptions> sourceOptionsList = new ArrayList<>();
                    opts.setSourceOptionsList(sourceOptionsList);
                    if (containsPaged)
                    {
                        PagedSourceOptions pagedSourceOptions = new PagedSourceOptions();
                        sourceOptionsList.add(pagedSourceOptions);
                        ifSet(options, START_PAGE, (v) -> pagedSourceOptions.setStartPageNumber(Integer.parseInt(v)));
                        ifSet(options, END_PAGE, (v) -> pagedSourceOptions.setEndPageNumber(Integer.parseInt(v)));
                        ifSet(options, PAGE, (v) ->
                        {
                            int i = Integer.parseInt(v);
                            pagedSourceOptions.setStartPageNumber(i);
                            pagedSourceOptions.setEndPageNumber(i);
                        });
                    }

                    if (containsCrop)
                    {
                        CropSourceOptions cropSourceOptions = new CropSourceOptions();
                        sourceOptionsList.add(cropSourceOptions);
                        ifSet(options, CROP_GRAVITY, (v) -> cropSourceOptions.setGravity(v));
                        ifSet(options, CROP_PERCENTAGE, (v) -> cropSourceOptions.setPercentageCrop(Boolean.parseBoolean(v)));
                        ifSet(options, CROP_WIDTH, (v) -> cropSourceOptions.setWidth(Integer.parseInt(v)));
                        ifSet(options, CROP_HEIGHT, (v) -> cropSourceOptions.setHeight(Integer.parseInt(v)));
                        ifSet(options, CROP_X_OFFSET, (v) -> cropSourceOptions.setXOffset(Integer.parseInt(v)));
                        ifSet(options, CROP_Y_OFFSET, (v) -> cropSourceOptions.setYOffset(Integer.parseInt(v)));
                    }

                    if (containsTemporal)
                    {
                        TemporalSourceOptions temporalSourceOptions = new TemporalSourceOptions();
                        sourceOptionsList.add(temporalSourceOptions);
                        ifSet(options, DURATION, (v) -> temporalSourceOptions.setDuration(v));
                        ifSet(options, OFFSET, (v) -> temporalSourceOptions.setOffset(v));
                    }
                }
            }
        }
        else
        {
            // This what the "pdf" rendition should have used in 6.0 and it is not unreasonable for a custom transformer
            // and rendition to do the same.
            transformationOptions = new TransformationOptions();
        }

        if (transformationOptions == null)
        {
            StringJoiner sj = new StringJoiner("\n    ");
            sj.add("The RenditionDefinition2 "+renditionName +
                    " contains options that cannot be mapped to TransformationOptions used by local transformers. "+
                    " The TransformOptionConverter may need to be sub classed to support this conversion.");
            HashSet<String> otherNames = new HashSet<>(optionNames);
            otherNames.removeAll(FLASH_OPTIONS);
            otherNames.removeAll(IMAGE_OPTIONS);
            otherNames.removeAll(PDF_OPTIONS);
            otherNames.removeAll(LIMIT_OPTIONS);
            otherNames.forEach(sj::add);
            sj.add("---");
            optionNames.forEach(sj::add);
            throw new IllegalArgumentException(sj.toString());
        }

        final TransformationOptions opts = transformationOptions;
        ifSet(options, INCLUDE_CONTENTS, (v) ->opts.setIncludeEmbedded(Boolean.parseBoolean(v)));

        if (containsAny(optionNames, LIMIT_OPTIONS))
        {
            TransformationOptionLimits limits = new TransformationOptionLimits();
            transformationOptions.setLimits(limits);
            ifSet(options, TIMEOUT, (v) -> limits.setTimeoutMs(Long.parseLong(v)));
            limits.setMaxSourceSizeKBytes(maxSourceSizeKBytes);
            limits.setReadLimitKBytes(readLimitTimeMs);
            limits.setReadLimitTimeMs(readLimitKBytes);
            limits.setMaxPages(maxPages);
            limits.setPageLimit(pageLimit);
        }

        transformationOptions.setUse(renditionName);
        return transformationOptions;
    }

    private <T> void ifSet(Map<String, String> options, String key, TransformationOptionsConverter.Setter setter)
    {
        String value = options.get(key);
        if (value != null)
        {
            setter.set(value);
        }
    }
}
