if (NOT DEFINED OB0E_SOURCE_DIR)
    message(FATAL_ERROR "OB0E_SOURCE_DIR must point to the fetched Oboe source")
endif ()

set(_source "${OB0E_SOURCE_DIR}/src/aaudio/AudioStreamAAudio.cpp")
if (NOT EXISTS "${_source}")
    message(FATAL_ERROR "Expected Oboe source not found: ${_source}")
endif ()

file(READ "${_source}" _contents)
set(_original [=[LOGE("%s, invalid parameters, %s", __func__, toString(parameters).c_str());]=])
set(_replacement [=[LOGE("%s, invalid parameters", __func__);]=])
string(FIND "${_contents}" "${_original}" _original_pos)

if (NOT _original_pos EQUAL -1)
    string(REPLACE "${_original}" "${_replacement}" _contents "${_contents}")
    file(WRITE "${_source}" "${_contents}")
else ()
    # A fresh checkout can already contain the patch (for example after an interrupted
    # configure). Accept that state, but fail loudly if upstream changes the call site.
    string(FIND "${_contents}" "${_replacement}" _replacement_pos)
    if (_replacement_pos EQUAL -1)
        message(FATAL_ERROR "Could not locate Oboe playback-parameter diagnostic to patch")
    endif ()
endif ()
