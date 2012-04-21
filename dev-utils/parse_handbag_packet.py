
# "abc;123;[5]a;b\nc\n" --> "abc", "123", "a;b\nc"

import sys

from StringIO import StringIO


class PacketParser:
    """
    """

    _stream = None


    def __init__(self, stream):
        """
        """
        self._stream = stream


    def nextPacket(self):
        """
        """

        charRead = ""

        field = ""

        all_fields = []

        chars_to_read = 0

        state = "firstchar" # /"normal" / "firstchar" 

        while 1:

            # TODO: Handle less data returned etc?
            charRead = self._stream.read(1)

            #print state, `field`, `charRead`

            if state == "firstchar":
                if charRead == "[":
                    chars_to_read = 0

                    # Read length & string here

                    while 1:
                        charRead = self._stream.read(1)

                        if charRead == "]":
                            break

                        # TODO: Bail if not digits?
                        chars_to_read = (10 * chars_to_read) + int(charRead)

                    # TODO: Read in one go?
                    for i in range(chars_to_read):
                        field += self._stream.read(1)

                     # TODO: Bail if not end of field or end of packet?
                    charRead = self._stream.read(1)

                state = "normal"

            if state == "normal":

                if (charRead == '\n') or (charRead == ';'):
                    all_fields.append(field);
                    field = ""
                    state = "firstchar"

                    if (charRead == '\n'):
                        break

                else:
                    field += charRead

        return all_fields



if __name__ == "__main__":

    if (len(sys.argv) > 1) and sys.argv[1] == "-":
        inputStream = sys.stdin
    else:
        inputStream = StringIO("abc;[12]abcdefghijkl;123;[5]a;b\nc\n")

    parser = PacketParser(inputStream)

    print parser.nextPacket()

        
