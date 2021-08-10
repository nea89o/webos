# Components of WebOs

## WebOS

A WebOS is the uppermost parent object of everything. It holds multiple console windows, one or more file systems, and other capabilities of the OS, such as whether network access, user control and system configurations.

## Console

A Console represents one access port to a [WebOS]. It can be a virtual console, which has no visual output port (potentially in the future used as a daemon), or one bound to a HTML Element, to which its output will be rendered.

## Activity

An Activity is a program which has direct access to a [Console]s rendering output, such as a shell which runs programs with only a STDOUT available.

## FileSystem

A FileSystem is composed of multiple [Mount]s to which it delegates file accesses to.

## Mount

A Mount processes individual file accesses and actions in its domain. It controls most files within its prefix, however it is not aware of it's prefix. A mount may be mounted at multiple paths.



[Console]: #console
[WebOS]: #webos
[Activity]: #activity
