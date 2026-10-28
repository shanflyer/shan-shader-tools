# Privacy

Shan HLSL has no analytics, advertising, accounts or cloud analysis service.
Shader text, edit notifications and language requests are sent to a local HlslTools
process through standard input/output. Analysis reads shader files, includes and
shadertoolsconfig.json on the local computer.

The bundled upstream server may write warning/error logs under the local temporary
directory. Such logs can contain diagnostic details and file paths. They are not
uploaded by this plugin. A user-configured replacement language server may behave
differently. JetBrains IDE services have their own privacy policies.
