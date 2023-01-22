package io.roach.pipeline.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.hateoas.UriTemplate;
import org.springframework.hateoas.config.EnableHypermediaSupport;
import org.springframework.hateoas.mediatype.hal.CurieProvider;
import org.springframework.hateoas.mediatype.hal.DefaultCurieProvider;
import org.springframework.hateoas.mediatype.hal.forms.HalFormsConfiguration;

import io.roach.pipeline.web.LinkRels;

@EnableHypermediaSupport(type = {
        EnableHypermediaSupport.HypermediaType.HAL_FORMS,
        EnableHypermediaSupport.HypermediaType.HAL
})
@Configuration
@ApplicationProfiles.Online
public class HypermediaConfiguration {
    @Bean
    public CurieProvider defaultCurieProvider() {
        return new DefaultCurieProvider(LinkRels.CURIE_NAMESPACE, UriTemplate.of("/rels/{rel}"));
    }

    @Bean
    public HalFormsConfiguration halFormsConfiguration() {
        return new HalFormsConfiguration();
    }
}
