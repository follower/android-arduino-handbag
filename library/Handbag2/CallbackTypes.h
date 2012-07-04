#ifndef _CALLBACK_TYPES_H_
#define _CALLBACK_TYPES_H_

// Basic callback is a no-argument, no return value callback.
// TODO: Rename?
#define BASIC_CALLBACK(varname) void (*varname)()

// Text callback is a single character string argument, no return value callback.
#define TEXT_CALLBACK(varname) void (*varname)(const char *)

enum CallbackType {
  NONE,
  BASIC,
  TEXT
};

#endif
