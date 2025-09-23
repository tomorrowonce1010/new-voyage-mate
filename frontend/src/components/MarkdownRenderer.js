import React from 'react';
import ReactMarkdown from 'react-markdown';
import remarkGfm from 'remark-gfm';
import './MarkdownRenderer.css';

const MarkdownRenderer = ({ content }) => {
  return (
    <div className="markdown-content">
      <ReactMarkdown 
        remarkPlugins={[remarkGfm]}
        components={{
          // 为不同的元素添加类名
          p: ({node, ...props}) => <p className="markdown-paragraph" {...props} />,
          h1: ({node, ...props}) => <h1 className="markdown-h1" {...props} />,
          h2: ({node, ...props}) => <h2 className="markdown-h2" {...props} />,
          h3: ({node, ...props}) => <h3 className="markdown-h3" {...props} />,
          h4: ({node, ...props}) => <h4 className="markdown-h4" {...props} />,
          h5: ({node, ...props}) => <h5 className="markdown-h5" {...props} />,
          h6: ({node, ...props}) => <h6 className="markdown-h6" {...props} />,
          strong: ({node, ...props}) => <strong className="markdown-strong" {...props} />,
          em: ({node, ...props}) => <em className="markdown-em" {...props} />,
          a: ({node, ...props}) => <a className="markdown-link" {...props} />,
          ul: ({node, ...props}) => <ul className="markdown-ul" {...props} />,
          ol: ({node, ...props}) => <ol className="markdown-ol" {...props} />,
          li: ({node, ...props}) => <li className="markdown-li" {...props} />,
          code: ({node, inline, ...props}) => 
            inline ? 
              <code className="markdown-inline-code" {...props} /> : 
              <code className="markdown-code-block" {...props} />,
          pre: ({node, ...props}) => <pre className="markdown-pre" {...props} />,
          blockquote: ({node, ...props}) => <blockquote className="markdown-blockquote" {...props} />,
          hr: ({node, ...props}) => <hr className="markdown-hr" {...props} />
        }}
      >
        {content}
      </ReactMarkdown>
    </div>
  );
};

export default MarkdownRenderer; 