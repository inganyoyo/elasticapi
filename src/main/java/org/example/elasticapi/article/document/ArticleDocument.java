package org.example.elasticapi.article.document;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class ArticleDocument {
    //    private String id;
    private String title;
    private String content;
    private List<String> attachments_content;
}