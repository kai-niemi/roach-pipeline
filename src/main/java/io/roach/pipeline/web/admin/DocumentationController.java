package io.roach.pipeline.web.admin;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@Controller
public class DocumentationController {
    @GetMapping("/rels/{rel}")
    public String rels(@PathVariable("rel") String rel, Model model) {
        model.addAttribute("title", rel);
        model.addAttribute("body", "");
        return "rel";
    }
}
