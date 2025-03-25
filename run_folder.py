import os
import subprocess
import sys
import platform

from checker import WaveOrderPicking

# Paths to the libraries
CPLEX_PATH = "$HOME/CPLEX_Studio2211/opl/bin/arm64_osx/"
OR_TOOLS_PATH = "$HOME/Documents/or-tools/build/lib/"

USE_CPLEX = False
USE_OR_TOOLS = False

MAX_RUNNING_TIME = "605s"


def compile_code(source_folder=os.getcwd()):
    print(f"Compiling code.")

    # Run Maven compile
    result = None

    try:
        result = subprocess.run(
            ["mvn", "clean", "package"],
            capture_output=True,
            text=True,
            shell=True,  # TODO: Change for linux
        )
    except Exception as e:
        print("mvn Error.")
        print(e)
        return False

    if result.returncode != 0:
        print("Maven compilation failed:")
        print(result.stderr)
        return False

    print("Maven compilation successful.")
    return True


def run_benchmark(dataset):

    input_folder = os.getcwd() + f"/datasets/{dataset}/"
    output_folder = os.getcwd() + f"/output/{dataset}/"

    if not os.path.exists(output_folder):
        os.makedirs(output_folder)

    # Set the library path (if needed)
    if USE_CPLEX and USE_OR_TOOLS:
        libraries = f"{OR_TOOLS_PATH}:{CPLEX_PATH}"
    elif USE_CPLEX:
        libraries = CPLEX_PATH
    elif USE_OR_TOOLS:
        libraries = OR_TOOLS_PATH

    if platform.system() == "Darwin":
        timeout_command = "gtimeout"
    else:
        timeout_command = "timeout"

    with open(output_folder + "result.out", "w") as file:
        pass

    for filename in os.listdir(input_folder):
        if filename.endswith(".txt"):
            print(f"Running {filename}")
            input_file = os.path.join(input_folder, filename)
            output_file = os.path.join(
                output_folder, f"{os.path.splitext(filename)[0]}.txt"
            )
            with open(output_file, "w") as out:
                # Main Java command
                cmd = [
                    # timeout_command, # TODO: Change for linux
                    # MAX_RUNNING_TIME, # TODO: Change for linux
                    "java",
                    "-Xmx16g",
                    "-jar",
                    "target/ChallengeSBPO2025-1.0.jar",
                    input_file,
                    output_file,
                ]
                if USE_CPLEX or USE_OR_TOOLS:
                    cmd.insert(3, f"-Djava.library.path={libraries}")

                result = subprocess.run(cmd, stderr=subprocess.PIPE, text=True)
                if result.returncode != 0:
                    print(f"Execution failed for {input_file}:")
                    print(result.stderr)
            with open(output_folder + "/result.out", "a") as file:
                file.write(str(fitness(input_file, output_file)) + "\n")


def fitness(input_file, output_file):
    wave_order_picking = WaveOrderPicking()
    wave_order_picking.read_input(input_file)
    selected_orders, visited_aisles = wave_order_picking.read_output(output_file)

    is_feasible = wave_order_picking.is_solution_feasible(
        selected_orders, visited_aisles
    )
    objective_value = wave_order_picking.compute_objective_function(
        selected_orders, visited_aisles
    )
    return objective_value if is_feasible else 0


if __name__ == "__main__":
    if len(sys.argv) != 2:
        print(sys.argv)
        print(
            "Usage: python run_challenge.py <dataset>\n",
            "Usage: python run_challenge.py a\n",
            "Usage: python run_challenge.py small\n",
        )
        sys.exit(1)

    dataset = sys.argv[1]

    if compile_code():
        run_benchmark(dataset)
