
# "abc;123;[5]a;b\nc\n" --> "abc", "123", "a;b\nc"

import sys

if __name__ == "__main__":

    data = "abc;[12]abcdefghijkl;123;[5]a;b\nc\n"

    if (len(sys.argv) > 1) and sys.argv[1] == "-":
        data = sys.stdin.read()

    offset = -1

    field = ""

    all_fields = []

    chars_to_read = 0

    state = "firstchar" # /"normal" / "firstchar" 

    while 1:

        offset += 1

        #print offset, state, `field`, `data[offset:]`

        if state == "firstchar":
            if data[offset] == "[":
                chars_to_read = 0

                # Read length & string here

                while 1:
                    offset += 1

                    if data[offset] == "]":
                        offset += 1 # Skip the ']'
                        break

                    # TODO: Bail if not digits?
                    chars_to_read = (10 * chars_to_read) + int(data[offset])

                for i in range(chars_to_read):
                    field += data[offset]
                    offset += 1

                 # TODO: Bail if not end of field or end of packet?

            state = "normal"

        if state == "normal":

            if (data[offset] == '\n') or (data[offset] == ';'):
                all_fields.append(field);
                field = ""
                state = "firstchar"

                if (data[offset] == '\n'):
                    break

            else:
                field += data[offset]
    
    print all_fields

        
