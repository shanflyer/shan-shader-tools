// Shan GLSL/HLSL integration additions, Copyright 2026 ShanFlyer. Apache-2.0.
using System.Collections.Generic;
using System.Linq;
using System.Threading;
using System.Threading.Tasks;
using MediatR;
using Microsoft.CodeAnalysis.Classification;
using Microsoft.CodeAnalysis.Text;
using OmniSharp.Extensions.JsonRpc;
using OmniSharp.Extensions.LanguageServer.Protocol;
using OmniSharp.Extensions.LanguageServer.Protocol.Client.Capabilities;
using OmniSharp.Extensions.LanguageServer.Protocol.Document;
using OmniSharp.Extensions.LanguageServer.Protocol.Models;
using OmniSharp.Extensions.LanguageServer.Server;
using ShaderTools.CodeAnalysis.Classification;
using ShaderTools.CodeAnalysis.Formatting;
using FormattingOptions = ShaderTools.CodeAnalysis.Formatting.FormattingOptions;

namespace ShaderTools.LanguageServer {
    internal static class ShanHandlers {
        public static LanguageServerOptions WithShanHandlers(this LanguageServerOptions options,
                LanguageServerWorkspace workspace, DocumentSelector selector) {
            options.AddHandler(new ShanFormattingHandler(workspace, selector));
            options.AddHandler(new ShanClassificationHandler(workspace));
            return options;
        }
    }

    // The original VS extension exposes this formatter through its VS integration.
    // Expose the same formatter over LSP for the CLion frontend.
    internal sealed class ShanFormattingHandler : IDocumentFormattingHandler {
        private readonly LanguageServerWorkspace workspace_;
        private readonly DocumentFormattingRegistrationOptions registration_;
        public ShanFormattingHandler(LanguageServerWorkspace workspace, DocumentSelector selector) {
            workspace_ = workspace;
            registration_ = new DocumentFormattingRegistrationOptions { DocumentSelector = selector };
        }

        public async Task<TextEditContainer> Handle(DocumentFormattingParams request, CancellationToken token) {
            var document = workspace_.GetDocument(request.TextDocument.Uri);
            if (document == null) { return new TextEditContainer(); }
            var options = (await document.GetOptionsAsync(token))
                .WithChangedOption(FormattingOptions.UseTabs, document.Language, !request.Options.InsertSpaces)
                .WithChangedOption(FormattingOptions.TabSize, document.Language, (int) request.Options.TabSize)
                .WithChangedOption(FormattingOptions.IndentationSize, document.Language, (int) request.Options.TabSize);
            var formatted = await Formatter.FormatAsync(document,
                new[] { new TextSpan(0, document.SourceText.Length) }, options, token);
            return new TextEditContainer(new TextEdit {
                Range = Helpers.ToRange(document.SourceText, new TextSpan(0, document.SourceText.Length)),
                NewText = formatted.SourceText.ToString()
            });
        }

        DocumentFormattingRegistrationOptions IRegistration<DocumentFormattingRegistrationOptions>.GetRegistrationOptions() => registration_;
        void ICapability<DocumentFormattingCapability>.SetCapability(DocumentFormattingCapability capability) {}
    }

    [Method("shaderTools/classifications", Direction.ClientToServer)]
    internal sealed class ShanClassificationParams : IRequest<ShanClassificationResult> {
        public TextDocumentIdentifier TextDocument { get; set; }
    }

    internal sealed class ShanClassificationResult {
        public string Text { get; set; } = "";
        public List<ShanClassificationSpan> Spans { get; set; } = new List<ShanClassificationSpan>();
    }

    internal sealed class ShanClassificationSpan {
        public int Start { get; set; }
        public int End { get; set; }
        public string Kind { get; set; }
    }

    internal sealed class ShanClassificationHandler : IJsonRpcRequestHandler<ShanClassificationParams, ShanClassificationResult> {
        private readonly LanguageServerWorkspace workspace_;
        public ShanClassificationHandler(LanguageServerWorkspace workspace) { workspace_ = workspace; }

        public async Task<ShanClassificationResult> Handle(ShanClassificationParams request, CancellationToken token) {
            var document = workspace_.GetDocument(request.TextDocument.Uri);
            if (document == null) { return new ShanClassificationResult(); }
            var tree = await document.GetSyntaxTreeAsync(token);
            var model = await document.GetSemanticModelAsync(token);
            var service = document.LanguageServices.GetService<IClassificationService>();
            var spans = new List<ClassifiedSpan>();
            var wholeFile = new TextSpan(0, document.SourceText.Length);
            service.AddSyntacticClassifications(tree, wholeFile, spans, token);
            service.AddSemanticClassifications(model, wholeFile, workspace_, spans, token);
            return new ShanClassificationResult {
                Text = document.SourceText.ToString(),
                Spans = spans.Where(span => span.TextSpan.Start >= 0 && span.TextSpan.End <= document.SourceText.Length)
                    .Select(span => new ShanClassificationSpan {
                        Start = span.TextSpan.Start, End = span.TextSpan.End, Kind = span.ClassificationType
                    }).ToList()
            };
        }
    }
}
