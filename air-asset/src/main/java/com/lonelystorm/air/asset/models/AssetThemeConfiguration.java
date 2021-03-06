package com.lonelystorm.air.asset.models;

import static java.lang.String.format;

import java.util.Calendar;
import java.util.Collection;
import java.util.Set;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.inject.Named;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.injectorspecific.Self;

import com.lonelystorm.air.asset.services.LibraryResolver;

@Model(adaptables = Resource.class)
public class AssetThemeConfiguration extends Asset {

    @Self
    private Resource resource;

    @Inject
    @Named("baseTheme")
    private String baseThemeRef;

    @Inject
    private String uniqueName;

    @Inject
    private LibraryResolver libResolver;

    @Inject
    private String themeConfigType;

    private AssetTheme baseTheme;

    private Calendar lastModified;

    public AssetTheme getBaseTheme() {
        return baseTheme;
    }

    @PostConstruct
    protected void construct() {
        this.setPath(resource.getPath());

        final Collection<AssetTheme> themes = libResolver.findThemesByTheme(baseThemeRef);
        if (themes == null || themes.size() < 1) {
            throw new IllegalArgumentException(format("No themes (%s) found for Theme Configuration at (%s)", baseThemeRef, resource.getPath()));
        }
        if (themes.size() > 1) {
            throw new IllegalArgumentException(format("Multiple theme assets (%s) found for Theme Configuration at (%s): %s", baseThemeRef, resource.getPath(), themes));
        }
        baseTheme = themes.iterator().next();
        Set<String> sources = baseTheme.getSources();
        if (sources == null || sources.size() < 1) {
            throw new IllegalArgumentException(format("No sources found for theme (%s) referenced by Theme Configuration at (%s)", baseThemeRef, resource.getPath()));
        }
        if (sources.size() > 1) {
            throw new IllegalArgumentException(format("Multiple theme sources found on theme (%s) referenced by Theme Configuration at (%s). Use @import rather than multiple sources: %s", baseThemeRef, resource.getPath(), sources));
        }

        int depth = 0;
        for (Resource node = resource; node != null && depth < 3; node = resource.getParent(), depth+= 1) {
            if (node.getValueMap().get("cq:lastModified", Calendar.class) != null) {
                lastModified = node.getValueMap().get("cq:lastModified", Calendar.class);
                break;
            }
        }

    }

    @Override
    public String[] getEmbed() {
        return baseTheme.getEmbed();
    }

    @Override
    public String[] getLoadPaths() {
        return baseTheme.getLoadPaths();
    }

    /** Guaranteed to only return one path **/
    @Override
    public Set<String> getSources() {
        return baseTheme.getSources();
    }

    public String getThemeSource() {
        return getSources().iterator().next();
    }

    public String getThemeConfigType() {
        return themeConfigType;
    }

    public String getBaseThemeName() {
        return baseThemeRef;
    }

    public String getUniqueName() {
        return uniqueName;
    }

    public String getRenderLocationPath() {
        return format("%s.%s", getBaseTheme().getPath(), getUniqueName());
    }

    public Calendar getLastModified() {
        return lastModified;
    }
}
