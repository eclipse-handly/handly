LSP Example
===========

The LSP example (`o.e.handly.examples.lsp*`) demonstrates a Handly-based
model that can represent in a generic way any LSP-based source file.
It is built on top of Eclipse LSP4J.

For information about LSP, see
<https://github.com/Microsoft/language-server-protocol>.

**Implementation note.** Currently, we use symbol location range to infer
a hierarchy for a given flat list of symbols in a text document, just as
Eclipse LSP4E does in its `SymbolModel`. However, according to a recent
revision of the LSP specification
 
> The range doesn't have to denote a node range in the sense of a abstract
> syntax tree. It can therefore not be used to re-construct a hierarchy of
> the symbols.
 
It appears that, for the time being, there is no protocol defined way in LSP
for building a tree of symbols [1]. Since the approach we currently use
clearly violates the specification, it may or may not work, depending on
a language server implementation.

[1]: https://github.com/Microsoft/language-server-protocol/issues/327