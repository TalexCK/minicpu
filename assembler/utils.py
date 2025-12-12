def read_file_as_list(path: str, encoding: str = "utf-8") -> list[str]:
    with open(path, "r", encoding=encoding) as f:
        return f.read().splitlines()


def write_file(path: str, lines: list, encoding: str = "utf-8") -> None:
    with open(path, "w", encoding=encoding) as f:
        f.write("".join(lines))
