from assembler import encode_code

while True:
    code = input("> ")
    correct_answer = input("> ")
    if encode_code(code) != correct_answer:
        print("Wrong!")
        print(encode_code(code))
        print(correct_answer)
    else:
        print("Correct!")