package org.ragbot.replica.bridge

/** A command for the tests, valid until 2100 unless told otherwise. */
internal fun command(
    operation: String,
    id: String = "cmd-1",
    destination: String? = null,
    form: String? = null,
    values: Map<String, FieldValue> = emptyMap(),
    arguments: Map<String, String> = emptyMap(),
    observation: Long = 1,
    draft: Long? = null,
    expires: String = "2100-01-01T00:00:00Z",
    device: String = "device-1",
) =
    Command(
        1,
        1,
        "task-1",
        1,
        id,
        device,
        "device-1",
        operation,
        observation,
        draft,
        expires,
        destination,
        form,
        values,
        arguments,
    )
