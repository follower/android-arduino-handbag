
import sys

if __name__ == "__main__":
    data = ["abcdef", 4567, "My goodness;\nI spot a newline!", 1]

    all_fields = []

    for item in data:
        #print item

        item = str(item)

        if (';' in item) or ('\n' in item):
            item = "[%d]%s" % (len(item), item)

        all_fields.append(item);

    output = "%s\n" % (";".join(all_fields))

    if (len(sys.argv) > 1) and sys.argv[1] == "-":
        print output
    else:
        print `output`

        
